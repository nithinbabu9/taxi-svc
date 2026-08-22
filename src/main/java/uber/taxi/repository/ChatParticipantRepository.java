package uber.taxi.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uber.taxi.entity.ChatParticipant;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {
    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);
    List<ChatParticipant> findByChatIdOrderByCreatedAtAsc(UUID chatId);
}
