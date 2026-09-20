package uber.taxi.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uber.taxi.config.MatchingProperties;
import uber.taxi.dto.match.RideMatchResponse;
import uber.taxi.entity.RideMatch;
import uber.taxi.entity.RideMatchStatus;
import uber.taxi.entity.RideRequest;
import uber.taxi.entity.RideRequestStatus;
import uber.taxi.exception.RideRequestNotFoundException;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.mapper.RideMatchMapper;
import uber.taxi.repository.RideMatchRepository;
import uber.taxi.repository.RideRequestRepository;
import uber.taxi.util.GeoDistanceCalculator;
import uber.taxi.util.GeoPoint;
import uber.taxi.util.PolylineDecoder;

@Service
public class RideMatchingService {

    private static final Logger log = LoggerFactory.getLogger(RideMatchingService.class);
    private final RideRequestRepository rideRepository;
    private final RideMatchRepository matchRepository;
    private final MatchingProperties properties;
    private final PolylineDecoder polylineDecoder;
    private final GeoDistanceCalculator distanceCalculator;
    private final RouteSimilarityCalculator similarityCalculator;
    private final RideMatchMapper matchMapper;

    public RideMatchingService(RideRequestRepository rideRepository, RideMatchRepository matchRepository,
                               MatchingProperties properties, PolylineDecoder polylineDecoder,
                               GeoDistanceCalculator distanceCalculator,
                               RouteSimilarityCalculator similarityCalculator, RideMatchMapper matchMapper) {
        this.rideRepository = rideRepository;
        this.matchRepository = matchRepository;
        this.properties = properties;
        this.polylineDecoder = polylineDecoder;
        this.distanceCalculator = distanceCalculator;
        this.similarityCalculator = similarityCalculator;
        this.matchMapper = matchMapper;
    }

    @Transactional
    public List<RideMatchResponse> generatePotentialMatches(RideRequest ride, boolean refreshExisting) {
        if (ride.getStatus() != RideRequestStatus.OPEN || isRouteDataMissing(ride)) {
            return List.of();
        }
        if (refreshExisting) {
            matchRepository.cancelPendingForRide(ride.getId(), RideMatchStatus.PENDING, RideMatchStatus.CANCELLED);
        }
        Duration window = properties.maximumDepartureTimeDifference();
        List<RideRequest> candidates = rideRepository.findMatchingCandidates(
                ride.getId(), ride.getUser().getId(), RideRequestStatus.OPEN,
                ride.getDepartureTime().minus(window), ride.getDepartureTime().plus(window));
        List<RideMatchResponse> results = new ArrayList<>();
        for (RideRequest candidate : candidates) {
            evaluate(ride, candidate)
                    .flatMap(evaluation -> saveOrRefresh(ride, candidate, evaluation))
                    .ifPresent(results::add);
        }
        results.sort(java.util.Comparator.comparingDouble(RideMatchResponse::matchScore).reversed());
        log.info("Found {} potential matches for ride {}", results.size(), ride.getId());
        return List.copyOf(results);
    }

    @Transactional(readOnly = true)
    public List<RideMatchResponse> getForRide(UUID actorId, UUID rideId) {
        RideRequest ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideRequestNotFoundException(rideId));
        if (!ride.getUser().getId().equals(actorId)) {
            throw new ForbiddenException("You do not own this ride request");
        }
        return matchRepository.findAllForRide(rideId).stream().map(matchMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RideMatchResponse> getForUser(UUID userId) {
        return matchRepository.findAllForUser(userId).stream().map(matchMapper::toResponse).toList();
    }

    @Transactional
    public void cancelPendingMatches(UUID rideId) {
        matchRepository.cancelPendingForRide(rideId, RideMatchStatus.PENDING, RideMatchStatus.CANCELLED);
    }

    private java.util.Optional<Evaluation> evaluate(RideRequest ride, RideRequest candidate) {
        if (isRouteDataMissing(candidate)) {
            return java.util.Optional.empty();
        }
        try {
            GeoPoint pickup = pickupOf(ride);
            GeoPoint candidatePickup = pickupOf(candidate);
            double pickupDistance = distanceCalculator.between(pickup, candidatePickup);
            double destinationDistance = distanceCalculator.between(destinationOf(ride), destinationOf(candidate));
            long timeDifference = Math.abs(Duration.between(ride.getDepartureTime(),
                    candidate.getDepartureTime()).getSeconds());
            List<GeoPoint> route = polylineDecoder.decode(ride.getRouteEncodedPolyline());
            List<GeoPoint> candidateRoute = polylineDecoder.decode(candidate.getRouteEncodedPolyline());
            double pickupToCandidateRoute = distanceCalculator.distanceToRoute(pickup, candidateRoute);
            double candidatePickupToRoute = distanceCalculator.distanceToRoute(candidatePickup, route);
            double routeSimilarity = similarityCalculator.calculate(route, candidateRoute,
                    properties.routeOverlapToleranceMeters());

            double directPickupScore = proximityScore(pickupDistance, properties.maximumPickupDeviationMeters());
            double joiningPickupScore = Math.max(
                    proximityScore(pickupToCandidateRoute, properties.maximumRoutePickupDeviationMeters()),
                    proximityScore(candidatePickupToRoute, properties.maximumRoutePickupDeviationMeters()));
            double pickupScore = Math.max(directPickupScore, joiningPickupScore);
            double destinationScore = proximityScore(destinationDistance,
                    properties.maximumDestinationDeviationMeters());
            double timeScore = proximityScore(timeDifference,
                    properties.maximumDepartureTimeDifference().getSeconds());

            boolean compatible = pickupScore > 0 && destinationScore > 0
                    && routeSimilarity >= properties.minimumRouteOverlap();
            if (!compatible) {
                log.info("Candidate {} rejected for ride {}: pickupScore={}, destinationScore={}, "
                                + "routeSimilarity={}, timeScore={}", candidate.getId(), ride.getId(),
                        pickupScore, destinationScore, routeSimilarity, timeScore);
                return java.util.Optional.empty();
            }
            double score = weightedScore(pickupScore, destinationScore, routeSimilarity, timeScore);
            if (score < properties.minimumMatchScore()) {
                log.info("Candidate {} rejected for ride {}: matchScore={} below minimum {}",
                        candidate.getId(), ride.getId(), score, properties.minimumMatchScore());
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new Evaluation(score, pickupDistance, destinationDistance,
                    routeSimilarity, timeDifference));
        } catch (IllegalArgumentException exception) {
            log.warn("Skipping candidate {} because stored route data is malformed", candidate.getId());
            return java.util.Optional.empty();
        }
    }

    private java.util.Optional<RideMatchResponse> saveOrRefresh(RideRequest ride, RideRequest candidate,
                                                                 Evaluation evaluation) {
        RideRequest first = ride.getId().compareTo(candidate.getId()) < 0 ? ride : candidate;
        RideRequest second = first == ride ? candidate : ride;
        java.util.Optional<RideMatch> existingMatch =
                matchRepository.findByRideRequest1IdAndRideRequest2Id(first.getId(), second.getId());
        if (existingMatch.isPresent()) {
            RideMatch existing = existingMatch.get();
            if (existing.getStatus() == RideMatchStatus.REJECTED
                    || existing.getStatus() == RideMatchStatus.ACCEPTED) {
                return java.util.Optional.empty();
            }
            existing.refresh(evaluation.score(), evaluation.pickupDistance(),
                    evaluation.destinationDistance(), evaluation.routeSimilarity(), evaluation.timeDifference());
            return java.util.Optional.of(matchMapper.toResponse(existing));
        }
        RideMatch created = new RideMatch(first, second, evaluation.score(), evaluation.pickupDistance(),
                evaluation.destinationDistance(), evaluation.routeSimilarity(), evaluation.timeDifference());
        return java.util.Optional.of(matchMapper.toResponse(matchRepository.save(created)));
    }

    private boolean isRouteDataMissing(RideRequest ride) {
        return ride.getPickupLatitude() == null || ride.getPickupLongitude() == null
                || ride.getDestinationLatitude() == null || ride.getDestinationLongitude() == null
                || ride.getRouteEncodedPolyline() == null;
    }

    private GeoPoint pickupOf(RideRequest ride) {
        return new GeoPoint(ride.getPickupLatitude().doubleValue(), ride.getPickupLongitude().doubleValue());
    }

    private GeoPoint destinationOf(RideRequest ride) {
        return new GeoPoint(ride.getDestinationLatitude().doubleValue(), ride.getDestinationLongitude().doubleValue());
    }

    private double proximityScore(double distance, double maximumDistance) {
        return Math.max(0, 1 - distance / maximumDistance);
    }

    private double weightedScore(double pickup, double destination, double overlap, double time) {
        double totalWeight = properties.pickupWeight() + properties.destinationWeight()
                + properties.routeOverlapWeight() + properties.departureTimeWeight();
        return (pickup * properties.pickupWeight() + destination * properties.destinationWeight()
                + overlap * properties.routeOverlapWeight() + time * properties.departureTimeWeight()) / totalWeight;
    }

    private record Evaluation(double score, double pickupDistance, double destinationDistance,
                              double routeSimilarity, long timeDifference) { }
}
