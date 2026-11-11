package uber.taxi.client;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public interface MapsClient {
    List<PlaceSuggestion> autocomplete(String input);

    GeocodedLocation geocode(String address);

    GeocodedLocation reverseGeocode(BigDecimal latitude, BigDecimal longitude);

    RouteDetails computeDrivingRoute(GeocodedLocation origin, GeocodedLocation destination, Instant departureTime);
}
