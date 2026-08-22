package uber.taxi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uber.taxi.config.MatchingProperties;
import uber.taxi.dto.match.RideMatchResponse;
import uber.taxi.entity.RideMatch;
import uber.taxi.entity.RideRequest;
import uber.taxi.entity.User;
import uber.taxi.mapper.RideMatchMapper;
import uber.taxi.repository.RideMatchRepository;
import uber.taxi.repository.RideRequestRepository;
import uber.taxi.util.GeoDistanceCalculator;
import uber.taxi.util.PolylineDecoder;

@ExtendWith(MockitoExtension.class)
class RideMatchingServiceTest {

    private static final String ROUTE = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";

    @Mock
    private RideRequestRepository rideRepository;
    @Mock
    private RideMatchRepository matchRepository;

    private RideMatchingService service;

    @BeforeEach
    void setUp() {
        MatchingProperties properties = new MatchingProperties(10_000, 10_000, 5_000,
                Duration.ofMinutes(30), 2_000, 0.25, 0.55, 0.25, 0.30, 0.30, 0.15);
        GeoDistanceCalculator distanceCalculator = new GeoDistanceCalculator();
        service = new RideMatchingService(rideRepository, matchRepository, properties, new PolylineDecoder(),
                distanceCalculator, new RouteSimilarityCalculator(distanceCalculator), new RideMatchMapper());
    }

    @Test
    void matchesPickupNearOtherUsersRouteEvenWhenDirectPickupsAreFarApart() throws Exception {
        Instant departure = Instant.parse("2027-01-01T12:00:00Z");
        RideRequest first = ride(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("10000000-0000-0000-0000-000000000001"), 38.5, -120.2, departure);
        RideRequest second = ride(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("10000000-0000-0000-0000-000000000002"), 40.7, -120.95,
                departure.plus(Duration.ofMinutes(10)));
        when(rideRepository.findMatchingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(second));
        when(matchRepository.findByRideRequest1IdAndRideRequest2Id(first.getId(), second.getId()))
                .thenReturn(Optional.empty());
        when(matchRepository.save(any(RideMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<RideMatchResponse> matches = service.generatePotentialMatches(first, false);

        assertEquals(1, matches.size());
        assertFalse(matches.getFirst().pickupDistanceMeters() < 10_000);
        assertEquals(1.0, matches.getFirst().routeSimilarity(), 0.0001);
    }

    @Test
    void rejectsCandidateWithDistantDestination() throws Exception {
        Instant departure = Instant.parse("2027-01-01T12:00:00Z");
        RideRequest first = ride(UUID.randomUUID(), UUID.randomUUID(), 38.5, -120.2, departure);
        RideRequest second = ride(UUID.randomUUID(), UUID.randomUUID(), 38.5, -120.2, departure);
        second.applyResolvedRoute("Pickup", BigDecimal.valueOf(38.5), BigDecimal.valueOf(-120.2), "p",
                "Far destination", BigDecimal.valueOf(20), BigDecimal.valueOf(20), "far",
                1_000, 600, ROUTE);
        when(rideRepository.findMatchingCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(second));

        assertEquals(List.of(), service.generatePotentialMatches(first, false));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void closedRideDoesNotSearchForCandidates() throws Exception {
        RideRequest ride = ride(UUID.randomUUID(), UUID.randomUUID(), 38.5, -120.2,
                Instant.parse("2027-01-01T12:00:00Z"));
        ride.cancel();

        assertEquals(List.of(), service.generatePotentialMatches(ride, false));
        verify(rideRepository, never()).findMatchingCandidates(any(), any(), any(), any(), any());
    }

    @Test
    void unresolvedRideDoesNotSearchForCandidates() throws Exception {
        User user = new User("Test", "User", "unresolved@example.com", null);
        setId(user, UUID.randomUUID());
        RideRequest ride = new RideRequest(user, "Pickup", "Destination",
                Instant.parse("2027-01-01T12:00:00Z"));
        setId(ride, UUID.randomUUID());

        assertEquals(List.of(), service.generatePotentialMatches(ride, false));
        verify(rideRepository, never()).findMatchingCandidates(any(), any(), any(), any(), any());
    }

    @Test
    void refreshCancelsPreviousPendingMatchesBeforeSearching() throws Exception {
        RideRequest ride = ride(UUID.randomUUID(), UUID.randomUUID(), 38.5, -120.2,
                Instant.parse("2027-01-01T12:00:00Z"));
        when(rideRepository.findMatchingCandidates(any(), any(), any(), any(), any())).thenReturn(List.of());

        service.generatePotentialMatches(ride, true);

        verify(matchRepository).cancelPendingForRide(
                ride.getId(), uber.taxi.entity.RideMatchStatus.PENDING,
                uber.taxi.entity.RideMatchStatus.CANCELLED);
    }

    private RideRequest ride(UUID rideId, UUID userId, double pickupLatitude, double pickupLongitude,
                             Instant departure) throws Exception {
        User user = new User("Test", "User", userId + "@example.com", null);
        setId(user, userId);
        RideRequest ride = new RideRequest(user, "Pickup", "Destination", departure);
        setId(ride, rideId);
        ride.applyResolvedRoute("Pickup", BigDecimal.valueOf(pickupLatitude), BigDecimal.valueOf(pickupLongitude),
                "pickup", "Destination", BigDecimal.valueOf(43.252), BigDecimal.valueOf(-126.453),
                "destination", 1_000, 600, ROUTE);
        return ride;
    }

    private void setId(Object target, UUID id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
