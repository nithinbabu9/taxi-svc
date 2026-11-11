package uber.taxi.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uber.taxi.dto.notification.NotificationResponse;
import uber.taxi.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getForUser(actorId(jwt));
    }

    @PostMapping("/{notificationId}/read")
    public void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificationId) {
        notificationService.markRead(actorId(jwt), notificationId);
    }

    @PostMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllRead(actorId(jwt));
    }

    private UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
