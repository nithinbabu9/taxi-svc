package uber.taxi.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uber.taxi.util.GeoDistanceCalculator;
import uber.taxi.util.GeoPoint;

/**
 * Deterministic offline map provider for local development. It deliberately avoids
 * network calls and must never be enabled outside the local Spring profile.
 */
@Component
@Profile("local")
public class LocalMapsClient implements MapsClient {

    private static final Logger log = LoggerFactory.getLogger(LocalMapsClient.class);
    private static final double ROAD_DISTANCE_FACTOR = 1.18;
    private static final double AVERAGE_SPEED_METERS_PER_SECOND = 17.88;
    private static final Map<String, Coordinates> KNOWN_LOCATIONS = Map.ofEntries(
            Map.entry("schaumburg, il", new Coordinates(42.0334, -88.0834)),
            Map.entry("arlington heights, il", new Coordinates(42.0884, -87.9806)),
            Map.entry("hoffman estates, il", new Coordinates(42.0629, -88.1227)),
            Map.entry("evanston, il", new Coordinates(42.0451, -87.6877)),
            Map.entry("downtown chicago", new Coordinates(41.8781, -87.6298)),
            Map.entry("downtown chicago, il", new Coordinates(41.8781, -87.6298)),
            Map.entry("millennium park, chicago, il", new Coordinates(41.8826, -87.6226)),
            Map.entry("milwaukee, wi", new Coordinates(43.0389, -87.9065))
    );

    private final GeoDistanceCalculator distanceCalculator;

    public LocalMapsClient(GeoDistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public GeocodedLocation geocode(String address) {
        String normalized = address.strip().toLowerCase(Locale.ROOT);
        Coordinates coordinates = KNOWN_LOCATIONS.getOrDefault(normalized, fallbackCoordinates(normalized));
        log.info("Using offline local geocoding data for address {}", address);
        return new GeocodedLocation(address.strip(), "local-" + Integer.toUnsignedString(normalized.hashCode()),
                decimal(coordinates.latitude()), decimal(coordinates.longitude()));
    }

    @Override
    public RouteDetails computeDrivingRoute(GeocodedLocation origin, GeocodedLocation destination,
                                             Instant departureTime) {
        double directDistance = distanceCalculator.between(
                new GeoPoint(origin.latitude().doubleValue(), origin.longitude().doubleValue()),
                new GeoPoint(destination.latitude().doubleValue(), destination.longitude().doubleValue()));
        long roadDistance = Math.max(1, Math.round(directDistance * ROAD_DISTANCE_FACTOR));
        long durationSeconds = Math.max(60, Math.round(roadDistance / AVERAGE_SPEED_METERS_PER_SECOND));
        return new RouteDetails(roadDistance, Duration.ofSeconds(durationSeconds),
                encodeInterpolatedRoute(origin, destination));
    }

    private Coordinates fallbackCoordinates(String address) {
        int hash = address.hashCode();
        double latitudeOffset = ((hash & 0xffff) / 65535.0 - 0.5) * 0.20;
        double longitudeOffset = (((hash >>> 16) & 0xffff) / 65535.0 - 0.5) * 0.20;
        return new Coordinates(41.8781 + latitudeOffset, -87.6298 + longitudeOffset);
    }

    private String encodeInterpolatedRoute(GeocodedLocation origin, GeocodedLocation destination) {
        List<Coordinates> points = new ArrayList<>();
        for (int step = 0; step <= 20; step++) {
            double ratio = step / 20.0;
            points.add(new Coordinates(
                    interpolate(origin.latitude().doubleValue(), destination.latitude().doubleValue(), ratio),
                    interpolate(origin.longitude().doubleValue(), destination.longitude().doubleValue(), ratio)));
        }
        StringBuilder encoded = new StringBuilder();
        int previousLatitude = 0;
        int previousLongitude = 0;
        for (Coordinates point : points) {
            int latitude = (int) Math.round(point.latitude() * 100_000);
            int longitude = (int) Math.round(point.longitude() * 100_000);
            encodeValue(latitude - previousLatitude, encoded);
            encodeValue(longitude - previousLongitude, encoded);
            previousLatitude = latitude;
            previousLongitude = longitude;
        }
        return encoded.toString();
    }

    private void encodeValue(int value, StringBuilder encoded) {
        int shifted = value < 0 ? ~(value << 1) : value << 1;
        while (shifted >= 0x20) {
            encoded.append((char) ((0x20 | (shifted & 0x1f)) + 63));
            shifted >>= 5;
        }
        encoded.append((char) (shifted + 63));
    }

    private double interpolate(double start, double end, double ratio) {
        return start + (end - start) * ratio;
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP);
    }

    private record Coordinates(double latitude, double longitude) { }
}
