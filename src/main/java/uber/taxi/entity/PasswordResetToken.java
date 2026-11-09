package uber.taxi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(UUID userId, String codeHash, Instant expiresAt) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }

    public void replaceCode(String newCodeHash, Instant newExpiresAt) {
        codeHash = newCodeHash;
        expiresAt = newExpiresAt;
        attempts = 0;
    }

    public void recordFailedAttempt() { attempts++; }
}
