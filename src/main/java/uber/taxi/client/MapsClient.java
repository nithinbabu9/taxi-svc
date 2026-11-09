package uber.taxi.client;

import java.time.Instant;
import java.util.List;

public interface MapsClient {
    List<PlaceSuggestion> autocomplete(String input);

    GeocodedLocation geocode(String address);

    RouteDetails computeDrivingRoute(GeocodedLocation origin, GeocodedLocation destination, Instant departureTime);
}
