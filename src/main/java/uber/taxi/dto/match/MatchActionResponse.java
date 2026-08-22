package uber.taxi.dto.match;

import java.util.UUID;
import uber.taxi.entity.RideMatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current match state after a decision; chatId is null until both users accept")
public record MatchActionResponse(UUID matchId, RideMatchStatus status,
                                  @Schema(nullable = true) UUID chatId) { }
