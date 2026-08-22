package uber.taxi.client;

import java.time.Instant;

public interface MapsClient {
    GeocodedLocation geocode(String address);

    RouteDetails computeDrivingRoute(GeocodedLocation origin, GeocodedLocation destination, Instant departureTime);
}
