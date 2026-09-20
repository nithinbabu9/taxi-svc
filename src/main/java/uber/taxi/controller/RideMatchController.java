package uber.taxi.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import uber.taxi.dto.match.RideMatchResponse;
import uber.taxi.service.RideMatchingService;

@RestController
@RequestMapping("/api/rides/{rideId}/matches")
@Tag(name = "Matches")
public class RideMatchController {

    private final RideMatchingService matchingService;

    public RideMatchController(RideMatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping
    @Operation(summary = "List potential matches for a ride", description = "Returns persisted matches ordered by score. An empty array means no candidate passed user, time, proximity, destination, overlap, and minimum-score checks.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Match list returned"), @ApiResponse(responseCode = "404", description = "Ride not found")})
    public List<RideMatchResponse> getMatches(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID rideId) {
        return matchingService.getForRide(UUID.fromString(jwt.getSubject()), rideId);
    }
}
