package uber.taxi.dto.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Private chat created exactly once for an accepted ride match")
public record ChatResponse(UUID id, UUID rideMatchId, List<UUID> participantIds, Instant createdAt) { }
