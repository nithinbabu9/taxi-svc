package uber.taxi.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
        @NotBlank String from,
        @NotNull Duration codeLifetime,
        @NotNull Duration resendCooldown,
        @Positive int maximumAttempts
) {
}
