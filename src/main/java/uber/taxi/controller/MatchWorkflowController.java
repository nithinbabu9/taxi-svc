package uber.taxi.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uber.taxi.dto.match.MatchActionRequest;
import uber.taxi.dto.match.MatchActionResponse;
import uber.taxi.service.MatchWorkflowService;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches")
public class MatchWorkflowController {

    private final MatchWorkflowService workflowService;

    public MatchWorkflowController(MatchWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/{matchId}/accept")
    @Operation(summary = "Accept a potential match", description = "Records acceptance for one participating user. The first acceptance leaves the match PENDING. The second acceptance atomically marks it ACCEPTED, marks both rides MATCHED, and creates exactly one private chat.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Decision recorded; chatId is populated after both accept"), @ApiResponse(responseCode = "400", description = "Acting user is not a participant"), @ApiResponse(responseCode = "404", description = "Match not found"), @ApiResponse(responseCode = "409", description = "Match or ride state prevents acceptance")})
    public MatchActionResponse accept(@PathVariable UUID matchId,
                                      @Valid @RequestBody MatchActionRequest request) {
        return workflowService.accept(matchId, request.userId());
    }

    @PostMapping("/{matchId}/reject")
    @Operation(summary = "Reject a potential match", description = "Marks a PENDING match REJECTED. Rejected matches cannot later be accepted.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Match rejected"), @ApiResponse(responseCode = "400", description = "Acting user is not a participant"), @ApiResponse(responseCode = "404", description = "Match not found"), @ApiResponse(responseCode = "409", description = "Match is already terminal")})
    public MatchActionResponse reject(@PathVariable UUID matchId,
                                      @Valid @RequestBody MatchActionRequest request) {
        return workflowService.reject(matchId, request.userId());
    }
}
