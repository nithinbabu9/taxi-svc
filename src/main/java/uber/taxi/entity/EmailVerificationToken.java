package uber.taxi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends BaseEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant sentAt;

    @Column(nullable = false)
    private int attempts;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(UUID userId, String codeHash, Instant expiresAt, Instant sentAt) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.sentAt = sentAt;
    }

    public UUID getUserId() { return userId; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getSentAt() { return sentAt; }
    public int getAttempts() { return attempts; }
    public void replaceCode(String newCodeHash, Instant newExpiresAt, Instant newSentAt) {
        codeHash = newCodeHash;
        expiresAt = newExpiresAt;
        sentAt = newSentAt;
        attempts = 0;
    }
    public void recordFailedAttempt() { attempts++; }
}
