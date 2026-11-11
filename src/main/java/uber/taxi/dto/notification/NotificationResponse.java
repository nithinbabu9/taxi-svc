package uber.taxi.dto.notification;

import java.time.Instant;
import java.util.UUID;
import uber.taxi.entity.NotificationType;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String body,
        UUID chatId,
        UUID rideMatchId,
        Instant readAt,
        Instant createdAt
) { }
