package uber.taxi.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uber.taxi.dto.notification.NotificationResponse;
import uber.taxi.entity.AppNotification;
import uber.taxi.entity.Chat;
import uber.taxi.entity.Message;
import uber.taxi.entity.NotificationType;
import uber.taxi.entity.RideMatch;
import uber.taxi.entity.User;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.exception.ResourceNotFoundException;
import uber.taxi.repository.AppNotificationRepository;

@Service
public class NotificationService {

    private final AppNotificationRepository notificationRepository;

    public NotificationService(AppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyMatchAccepted(User recipient, RideMatch match) {
        save(recipient, NotificationType.MATCH_ACCEPTED, "A traveler accepted your match",
                "Open your matches to accept and start coordinating your ride.", null, match);
    }

    @Transactional
    public void notifyChatReady(User recipient, Chat chat) {
        save(recipient, NotificationType.CHAT_READY, "Your ride chat is ready",
                "Both travelers accepted the route match. You can now coordinate privately.", chat,
                chat.getRideMatch());
    }

    @Transactional
    public void notifyNewMessage(User recipient, Message message) {
        String senderName = message.getSender().getFirstName();
        save(recipient, NotificationType.NEW_MESSAGE, "New message from " + senderName,
                abbreviate(message.getText()), message.getChat(), message.getChat().getRideMatch());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(UUID userId) {
        return notificationRepository.findTop30ByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findTop30ByRecipientIdOrderByCreatedAtDesc(userId)
                .forEach(notification -> notification.markRead(Instant.now()));
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this notification");
        }
        notification.markRead(Instant.now());
    }

    private void save(User recipient, NotificationType type, String title, String body,
                      Chat chat, RideMatch rideMatch) {
        notificationRepository.save(new AppNotification(recipient, type, title, body, chat, rideMatch));
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getChat() == null ? null : notification.getChat().getId(),
                notification.getRideMatch() == null ? null : notification.getRideMatch().getId(),
                notification.getReadAt(), notification.getCreatedAt());
    }

    private String abbreviate(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 177) + "…";
    }
}
