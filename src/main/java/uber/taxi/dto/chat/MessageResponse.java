package uber.taxi.dto.chat;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persisted chat message")
public record MessageResponse(UUID id, UUID chatId, UUID senderId, String message,
                              Instant sentAt, Instant readAt) { }
