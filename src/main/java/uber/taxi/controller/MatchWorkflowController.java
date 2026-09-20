package uber.taxi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import uber.taxi.dto.match.MatchActionResponse;
import uber.taxi.dto.match.RideMatchResponse;
import uber.taxi.service.MatchWorkflowService;
import uber.taxi.service.RideMatchingService;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches")
public class MatchWorkflowController {

    private final MatchWorkflowService workflowService;
    private final RideMatchingService matchingService;

    public MatchWorkflowController(MatchWorkflowService workflowService, RideMatchingService matchingService) {
        this.workflowService = workflowService;
        this.matchingService = matchingService;
    }

    @GetMapping("/mine")
    @Operation(summary = "List my ranked ride matches", description = "Returns every match involving an OPEN, MATCHED, or historical ride owned by the authenticated traveler, ordered by compatibility score.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Match list returned"))
    public List<RideMatchResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return matchingService.getForUser(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/{matchId}/accept")
    @Operation(summary = "Accept a potential match", description = "Records acceptance for one participating user. The first acceptance leaves the match PENDING. The second acceptance atomically marks it ACCEPTED, marks both rides MATCHED, and creates exactly one private chat.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Decision recorded; chatId is populated after both accept"), @ApiResponse(responseCode = "403", description = "Authenticated user is not a participant"), @ApiResponse(responseCode = "404", description = "Match not found"), @ApiResponse(responseCode = "409", description = "Match or ride state prevents acceptance")})
    public MatchActionResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID matchId) {
        return workflowService.accept(matchId, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/{matchId}/reject")
    @Operation(summary = "Reject a potential match", description = "Marks a PENDING match REJECTED. Rejected matches cannot later be accepted.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Match rejected"), @ApiResponse(responseCode = "403", description = "Authenticated user is not a participant"), @ApiResponse(responseCode = "404", description = "Match not found"), @ApiResponse(responseCode = "409", description = "Match is already terminal")})
    public MatchActionResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID matchId) {
        return workflowService.reject(matchId, UUID.fromString(jwt.getSubject()));
    }
}
