package uber.taxi.client;

import java.math.BigDecimal;

public record GeocodedLocation(
        String formattedAddress,
        String placeId,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
