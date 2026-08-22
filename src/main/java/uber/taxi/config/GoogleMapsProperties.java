package uber.taxi.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.google-maps")
public record GoogleMapsProperties(
        @NotBlank String apiKey,
        @NotNull URI geocodingBaseUrl,
        @NotNull URI routesBaseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
