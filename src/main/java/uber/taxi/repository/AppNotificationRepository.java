package uber.taxi.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uber.taxi.entity.AppNotification;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {
    List<AppNotification> findTop30ByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
}
