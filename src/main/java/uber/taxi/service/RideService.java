package uber.taxi.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uber.taxi.dto.ride.CreateRideRequest;
import uber.taxi.dto.ride.RideResponse;
import uber.taxi.dto.ride.UpdateRideRequest;
import uber.taxi.entity.RideRequest;
import uber.taxi.entity.RideRequestStatus;
import uber.taxi.entity.User;
import uber.taxi.exception.ConflictException;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.exception.InvalidRequestException;
import uber.taxi.exception.RideRequestNotFoundException;
import uber.taxi.mapper.RideMapper;
import uber.taxi.repository.RideRequestRepository;
import uber.taxi.client.GeocodedLocation;
import uber.taxi.client.MapsClient;
import uber.taxi.client.RouteDetails;

@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);
    private final RideRequestRepository rideRepository;
    private final UserService userService;
    private final RideMapper rideMapper;
    private final MapsClient mapsClient;
    private final RideMatchingService matchingService;

    public RideService(RideRequestRepository rideRepository, UserService userService, RideMapper rideMapper,
                       MapsClient mapsClient, RideMatchingService matchingService) {
        this.rideRepository = rideRepository;
        this.userService = userService;
        this.rideMapper = rideMapper;
        this.mapsClient = mapsClient;
        this.matchingService = matchingService;
    }

    @Transactional
    public RideResponse create(UUID actorId, CreateRideRequest request) {
        validateDifferentLocations(request.pickupAddress(), request.destinationAddress());
        User user = userService.findEntity(actorId);
        ResolvedTrip trip = resolveTrip(request.pickupAddress(), request.destinationAddress(), request.departureTime());
        RideRequest ride = new RideRequest(user, request.pickupAddress().trim(),
                request.destinationAddress().trim(), request.departureTime());
        applyResolvedTrip(ride, trip);
        rideRepository.save(ride);
        matchingService.generatePotentialMatches(ride, false);
        log.info("Ride request created with id {} for user {}", ride.getId(), user.getId());
        return rideMapper.toResponse(ride);
    }

    @Transactional(readOnly = true)
    public RideResponse get(UUID actorId, UUID id) {
        return rideMapper.toResponse(requireOwner(actorId, findEntity(id)));
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getForUser(UUID actorId) {
        userService.findEntity(actorId);
        return rideRepository.findByUserIdOrderByDepartureTimeDesc(actorId).stream()
                .map(rideMapper::toResponse)
                .toList();
    }

    @Transactional
    public RideResponse update(UUID actorId, UUID id, UpdateRideRequest request) {
        validateDifferentLocations(request.pickupAddress(), request.destinationAddress());
        RideRequest ride = requireOwner(actorId, findEntity(id));
        requireOpen(ride, "updated");
        ResolvedTrip trip = resolveTrip(request.pickupAddress(), request.destinationAddress(), request.departureTime());
        ride.update(request.pickupAddress().trim(), request.destinationAddress().trim(), request.departureTime());
        applyResolvedTrip(ride, trip);
        matchingService.generatePotentialMatches(ride, true);
        log.info("Ride request updated with id {}", id);
        return rideMapper.toResponse(ride);
    }

    @Transactional
    public RideResponse cancel(UUID actorId, UUID id) {
        RideRequest ride = requireOwner(actorId, findEntity(id));
        requireOpen(ride, "cancelled");
        ride.cancel();
        matchingService.cancelPendingMatches(id);
        log.info("Ride request cancelled with id {}", id);
        return rideMapper.toResponse(ride);
    }

    private RideRequest findEntity(UUID id) {
        return rideRepository.findById(id).orElseThrow(() -> new RideRequestNotFoundException(id));
    }

    private RideRequest requireOwner(UUID actorId, RideRequest ride) {
        if (!ride.getUser().getId().equals(actorId)) {
            throw new ForbiddenException("You do not own this ride request");
        }
        return ride;
    }

    private void requireOpen(RideRequest ride, String operation) {
        if (ride.getStatus() != RideRequestStatus.OPEN) {
            throw new ConflictException("Only an OPEN ride request can be " + operation);
        }
    }

    private void validateDifferentLocations(String pickup, String destination) {
        String normalizedPickup = pickup.trim().toLowerCase(Locale.ROOT);
        String normalizedDestination = destination.trim().toLowerCase(Locale.ROOT);
        if (normalizedPickup.equals(normalizedDestination)) {
            throw new InvalidRequestException("Pickup and destination must be different");
        }
    }

    private ResolvedTrip resolveTrip(String pickupAddress, String destinationAddress, java.time.Instant departureTime) {
        GeocodedLocation pickup = mapsClient.geocode(pickupAddress.trim());
        GeocodedLocation destination = mapsClient.geocode(destinationAddress.trim());
        RouteDetails route = mapsClient.computeDrivingRoute(pickup, destination, departureTime);
        return new ResolvedTrip(pickup, destination, route);
    }

    private void applyResolvedTrip(RideRequest ride, ResolvedTrip trip) {
        ride.applyResolvedRoute(
                trip.pickup().formattedAddress(), trip.pickup().latitude(), trip.pickup().longitude(),
                trip.pickup().placeId(), trip.destination().formattedAddress(), trip.destination().latitude(),
                trip.destination().longitude(), trip.destination().placeId(), trip.route().distanceMeters(),
                trip.route().duration().getSeconds(), trip.route().encodedPolyline());
    }

    private record ResolvedTrip(GeocodedLocation pickup, GeocodedLocation destination, RouteDetails route) { }
}
