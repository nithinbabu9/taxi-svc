package uber.taxi.dto.auth;

import java.time.Instant;

public record VerificationPendingResponse(String email, Instant expiresAt, boolean verificationRequired) {
}
