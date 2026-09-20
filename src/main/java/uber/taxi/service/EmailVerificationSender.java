package uber.taxi.service;

import java.time.Instant;

public interface EmailVerificationSender {
    void sendVerificationCode(String recipient, String code, Instant expiresAt);
}
