package uber.taxi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_notifications", indexes = {
        @Index(name = "idx_app_notifications_recipient_created", columnList = "recipient_id,created_at"),
        @Index(name = "idx_app_notifications_recipient_unread", columnList = "recipient_id,read_at")
})
public class AppNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_match_id")
    private RideMatch rideMatch;

    @Column(name = "read_at")
    private Instant readAt;

    @SuppressWarnings("unused") // Required by JPA.
    protected AppNotification() {
    }

    public AppNotification(User recipient, NotificationType type, String title, String body,
                           Chat chat, RideMatch rideMatch) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.body = body;
        this.chat = chat;
        this.rideMatch = rideMatch;
    }

    public void markRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }

    public UUID getId() { return id; }
    public User getRecipient() { return recipient; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Chat getChat() { return chat; }
    public RideMatch getRideMatch() { return rideMatch; }
    public Instant getReadAt() { return readAt; }
}
