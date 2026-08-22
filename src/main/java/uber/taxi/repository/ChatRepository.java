package uber.taxi.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uber.taxi.entity.Chat;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    Optional<Chat> findByRideMatchId(UUID rideMatchId);

    @Query("""
            select distinct chat from ChatParticipant participant
            join participant.chat chat
            join fetch chat.rideMatch
            where participant.user.id = :userId
            order by chat.createdAt desc
            """)
    List<Chat> findAllForUser(@Param("userId") UUID userId);
}
