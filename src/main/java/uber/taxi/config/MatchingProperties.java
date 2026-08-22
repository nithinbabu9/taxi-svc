package uber.taxi.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.matching")
public record MatchingProperties(
        @Positive long maximumPickupDeviationMeters,
        @Positive long maximumDestinationDeviationMeters,
        @Positive long maximumRoutePickupDeviationMeters,
        @NotNull Duration maximumDepartureTimeDifference,
        @Positive long routeOverlapToleranceMeters,
        @DecimalMin("0.0") @DecimalMax("1.0") double minimumRouteOverlap,
        @DecimalMin("0.0") @DecimalMax("1.0") double minimumMatchScore,
        @Positive double pickupWeight,
        @Positive double destinationWeight,
        @Positive double routeOverlapWeight,
        @Positive double departureTimeWeight
) { }
