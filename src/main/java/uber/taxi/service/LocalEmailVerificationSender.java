package uber.taxi.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class LocalEmailVerificationSender implements EmailVerificationSender {
    private static final Logger log = LoggerFactory.getLogger(LocalEmailVerificationSender.class);
    private final Map<String, String> latestCodes = new ConcurrentHashMap<>();

    @Override
    public void sendVerificationCode(String recipient, String code, Instant expiresAt) {
        latestCodes.put(recipient, code);
        log.info("LOCAL ONLY email verification code for {}: {} (expires {})", recipient, code, expiresAt);
    }

    public String latestCodeFor(String recipient) {
        return latestCodes.get(recipient);
    }
}
