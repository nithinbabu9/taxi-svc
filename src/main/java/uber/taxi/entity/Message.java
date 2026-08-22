package uber.taxi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_chat_sent", columnList = "chat_id,sent_at"),
        @Index(name = "idx_messages_sender", columnList = "sender_id")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message", nullable = false, length = 4000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    @SuppressWarnings("unused") // Populated by JPA; write support is part of the future read-receipt workflow.
    private Instant readAt;

    @SuppressWarnings("unused") // Required by JPA.
    protected Message() { }

    public Message(Chat chat, User sender, String text) {
        this.chat = chat;
        this.sender = sender;
        this.text = text;
        this.sentAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Chat getChat() { return chat; }
    public User getSender() { return sender; }
    public String getText() { return text; }
    public Instant getSentAt() { return sentAt; }
    public Instant getReadAt() { return readAt; }
}
