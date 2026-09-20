package uber.taxi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.Duration;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uber.taxi.dto.ride.CreateRideRequest;
import uber.taxi.entity.RideRequest;
import uber.taxi.entity.RideRequestStatus;
import uber.taxi.entity.User;
import uber.taxi.exception.ConflictException;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.exception.InvalidRequestException;
import uber.taxi.mapper.RideMapper;
import uber.taxi.repository.RideRequestRepository;
import uber.taxi.client.MapsClient;
import uber.taxi.client.GeocodedLocation;
import uber.taxi.client.RouteDetails;
import uber.taxi.dto.ride.RideResponse;
import uber.taxi.dto.ride.UpdateRideRequest;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRequestRepository rideRepository;
    @Mock
    private UserService userService;
    @Mock
    private MapsClient mapsClient;
    @Mock
    private RideMatchingService matchingService;

    private RideService rideService;

    @BeforeEach
    void setUp() {
        rideService = new RideService(rideRepository, userService, new RideMapper(), mapsClient, matchingService);
    }

    @Test
    void rejectsEffectivelyIdenticalLocationsBeforeSaving() {
        CreateRideRequest request = new CreateRideRequest(" Chicago ", "chicago",
                Instant.now().plus(1, ChronoUnit.DAYS));

        assertThrows(InvalidRequestException.class, () -> rideService.create(UUID.randomUUID(), request));
        verify(rideRepository, never()).save(any());
    }

    @Test
    void createsRideFromServerResolvedLocationsAndRoute() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant departure = Instant.now().plus(1, ChronoUnit.DAYS);
        User user = new User("Test", "Person", "person@example.com", null);
        setId(user, userId);
        GeocodedLocation pickup = new GeocodedLocation("Schaumburg, IL, USA", "pickup-place",
                new BigDecimal("42.0334"), new BigDecimal("-88.0834"));
        GeocodedLocation destination = new GeocodedLocation("Chicago, IL, USA", "destination-place",
                new BigDecimal("41.8781"), new BigDecimal("-87.6298"));
        when(userService.findEntity(userId)).thenReturn(user);
        when(mapsClient.geocode("Schaumburg, IL")).thenReturn(pickup);
        when(mapsClient.geocode("Chicago, IL")).thenReturn(destination);
        when(mapsClient.computeDrivingRoute(pickup, destination, departure))
                .thenReturn(new RouteDetails(48_000, Duration.ofMinutes(45), "polyline"));
        when(rideRepository.save(any(RideRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RideResponse response = rideService.create(userId,
                new CreateRideRequest("Schaumburg, IL", "Chicago, IL", departure));

        assertEquals("Schaumburg, IL, USA", response.pickupAddress());
        assertEquals("pickup-place", response.pickupPlaceId());
        assertEquals(48_000, response.routeDistanceMeters());
        assertEquals(2_700, response.routeDurationSeconds());
    }

    @Test
    void cancellationChangesStatusInsteadOfDeleting() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User user = new User("Test", "Person", "person@example.com", null);
        setId(user, actorId);
        RideRequest ride = new RideRequest(user,
                "Schaumburg, IL", "Chicago, IL", Instant.now().plus(1, ChronoUnit.DAYS));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        rideService.cancel(actorId, rideId);

        assertEquals(RideRequestStatus.CANCELLED, ride.getStatus());
        verify(rideRepository, never()).delete(any());
    }

    @Test
    void cannotCancelRideTwice() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User user = new User("Test", "Person", "person@example.com", null);
        setId(user, actorId);
        RideRequest ride = new RideRequest(user,
                "Schaumburg, IL", "Chicago, IL", Instant.now().plus(1, ChronoUnit.DAYS));
        ride.cancel();
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThrows(ConflictException.class, () -> rideService.cancel(actorId, rideId));
    }

    @Test
    void updateReplacesResolvedRouteAndRefreshesMatches() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
        User user = new User("Test", "Person", "person@example.com", null);
        setId(user, actorId);
        RideRequest ride = new RideRequest(user,
                "Old pickup", "Old destination", departure.minus(1, ChronoUnit.DAYS));
        GeocodedLocation pickup = new GeocodedLocation("Evanston, IL", "evanston",
                new BigDecimal("42.0451"), new BigDecimal("-87.6877"));
        GeocodedLocation destination = new GeocodedLocation("Milwaukee, WI", "milwaukee",
                new BigDecimal("43.0389"), new BigDecimal("-87.9065"));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));
        when(mapsClient.geocode("Evanston, IL")).thenReturn(pickup);
        when(mapsClient.geocode("Milwaukee, WI")).thenReturn(destination);
        when(mapsClient.computeDrivingRoute(pickup, destination, departure))
                .thenReturn(new RouteDetails(130_000, Duration.ofMinutes(80), "updated-polyline"));

        RideResponse response = rideService.update(actorId, rideId,
                new UpdateRideRequest(" Evanston, IL ", " Milwaukee, WI ", departure));

        assertEquals("Evanston, IL", response.pickupAddress());
        assertEquals("Milwaukee, WI", response.destinationAddress());
        assertEquals(130_000, response.routeDistanceMeters());
        verify(matchingService).generatePotentialMatches(ride, true);
    }

    @Test
    void matchedRideCannotBeUpdated() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User user = new User("Test", "Person", "person@example.com", null);
        setId(user, actorId);
        RideRequest ride = new RideRequest(user,
                "Pickup", "Destination", Instant.now().plus(1, ChronoUnit.DAYS));
        ride.markMatched();
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThrows(ConflictException.class, () -> rideService.update(actorId, rideId,
                new UpdateRideRequest("New pickup", "New destination", Instant.now().plus(2, ChronoUnit.DAYS))));
        verify(mapsClient, never()).geocode(any());
    }

    @Test
    void preventsAnotherUserFromCancellingARide() throws Exception {
        UUID rideId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User owner = new User("Owner", "User", "owner@example.com", null);
        setId(owner, ownerId);
        RideRequest ride = new RideRequest(owner, "Schaumburg, IL", "Chicago, IL",
                Instant.now().plus(1, ChronoUnit.DAYS));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThrows(ForbiddenException.class, () -> rideService.cancel(UUID.randomUUID(), rideId));
        assertEquals(RideRequestStatus.OPEN, ride.getStatus());
    }

    private void setId(Object target, UUID id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
