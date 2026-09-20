package uber.taxi.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uber.taxi.dto.ride.CreateRideRequest;
import uber.taxi.dto.ride.RideResponse;
import uber.taxi.dto.ride.UpdateRideRequest;
import uber.taxi.service.RideService;

@RestController
@RequestMapping("/api")
@Tag(name = "Rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/rides")
    @Operation(summary = "Create a ride request", description = "Geocodes pickup/destination, calculates a driving route, stores the OPEN ride, and immediately evaluates compatible OPEN rides owned by other users. departureTime must be a future ISO-8601 instant.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Ride created"), @ApiResponse(responseCode = "400", description = "Invalid request or location"), @ApiResponse(responseCode = "404", description = "User not found"), @ApiResponse(responseCode = "502", description = "Map provider failed"), @ApiResponse(responseCode = "503", description = "Map provider unavailable or quota exceeded")})
    public ResponseEntity<RideResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateRideRequest request) {
        RideResponse response = rideService.create(actorId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/rides/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/rides/{id}")
    @Operation(summary = "Get a ride request")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Ride found"), @ApiResponse(responseCode = "404", description = "Ride not found")})
    public RideResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return rideService.get(actorId(jwt), id);
    }

    @GetMapping("/rides/mine")
    @Operation(summary = "List a user's rides", description = "Returns all ride requests owned by the supplied user.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Ride list returned"), @ApiResponse(responseCode = "404", description = "User not found")})
    public List<RideResponse> getForUser(@AuthenticationPrincipal Jwt jwt) {
        return rideService.getForUser(actorId(jwt));
    }

    @PutMapping("/rides/{id}")
    @Operation(summary = "Update an open ride", description = "Re-resolves locations and route data, cancels stale pending matches, and reruns matching. Only OPEN rides can be updated.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Ride updated"), @ApiResponse(responseCode = "400", description = "Validation or location failure"), @ApiResponse(responseCode = "404", description = "Ride not found"), @ApiResponse(responseCode = "409", description = "Ride is not open")})
    public RideResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateRideRequest request) {
        return rideService.update(actorId(jwt), id, request);
    }

    @DeleteMapping("/rides/{id}")
    @Operation(summary = "Cancel a ride", description = "Performs a soft cancellation by changing status to CANCELLED and cancelling pending matches; no database row is deleted.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Ride cancelled"), @ApiResponse(responseCode = "404", description = "Ride not found"), @ApiResponse(responseCode = "409", description = "Ride cannot be cancelled in its current state")})
    public RideResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return rideService.cancel(actorId(jwt), id);
    }

    private UUID actorId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
