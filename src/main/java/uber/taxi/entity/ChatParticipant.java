package uber.taxi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "chat_participants", uniqueConstraints =
        @UniqueConstraint(name = "uk_chat_participants_chat_user", columnNames = {"chat_id", "user_id"}))
public class ChatParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @SuppressWarnings("unused") // Required by JPA.
    protected ChatParticipant() { }

    public ChatParticipant(Chat chat, User user) {
        this.chat = chat;
        this.user = user;
    }

    public UUID getId() { return id; }
    public Chat getChat() { return chat; }
    public User getUser() { return user; }
}
