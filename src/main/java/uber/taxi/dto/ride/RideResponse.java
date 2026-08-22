package uber.taxi.dto.ride;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import uber.taxi.entity.RideRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stored ride including resolved coordinates and compact route data")
public record RideResponse(
        UUID id,
        UUID userId,
        String pickupAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        String pickupPlaceId,
        String destinationAddress,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,
        String destinationPlaceId,
        Long routeDistanceMeters,
        Long routeDurationSeconds,
        String routeEncodedPolyline,
        Instant departureTime,
        RideRequestStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
