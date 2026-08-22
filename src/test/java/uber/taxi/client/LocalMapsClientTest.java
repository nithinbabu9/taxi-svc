package uber.taxi.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import uber.taxi.util.GeoDistanceCalculator;
import uber.taxi.util.PolylineDecoder;

class LocalMapsClientTest {

    private final LocalMapsClient client = new LocalMapsClient(new GeoDistanceCalculator());

    @Test
    void providesDeterministicOfflineLocationAndRoute() {
        GeocodedLocation pickup = client.geocode("Schaumburg, IL");
        GeocodedLocation destination = client.geocode("Millennium Park, Chicago, IL");

        RouteDetails route = client.computeDrivingRoute(pickup, destination, Instant.now());

        assertThat(pickup.placeId()).startsWith("local-");
        assertThat(route.distanceMeters()).isPositive();
        assertThat(route.duration()).isPositive();
        assertThat(new PolylineDecoder().decode(route.encodedPolyline())).hasSize(21);
    }

    @Test
    void unknownAddressUsesStableFallbackCoordinates() {
        GeocodedLocation first = client.geocode(" 123 Example Street ");
        GeocodedLocation second = client.geocode("123 example street");

        assertThat(first.latitude()).isEqualByComparingTo(second.latitude());
        assertThat(first.longitude()).isEqualByComparingTo(second.longitude());
        assertThat(first.formattedAddress()).isEqualTo("123 Example Street");
        assertThat(first.placeId()).isEqualTo(second.placeId());
    }
}
