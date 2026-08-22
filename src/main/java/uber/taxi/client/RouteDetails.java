package uber.taxi.client;

import java.time.Duration;

public record RouteDetails(
        long distanceMeters,
        Duration duration,
        String encodedPolyline
) {
}
