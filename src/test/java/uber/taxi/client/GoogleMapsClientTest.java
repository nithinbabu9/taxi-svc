package uber.taxi.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uber.taxi.config.GoogleMapsProperties;
import uber.taxi.exception.GoogleMapsServiceException;
import uber.taxi.exception.InvalidLocationException;
import uber.taxi.exception.NoRouteFoundException;

class GoogleMapsClientTest {

    private MockRestServiceServer geocodingServer;
    private MockRestServiceServer routesServer;
    private GoogleMapsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder geocodingBuilder = RestClient.builder().baseUrl("https://geocode.test");
        RestClient.Builder routesBuilder = RestClient.builder().baseUrl("https://routes.test");
        geocodingServer = MockRestServiceServer.bindTo(geocodingBuilder).build();
        routesServer = MockRestServiceServer.bindTo(routesBuilder).build();
        GoogleMapsProperties properties = new GoogleMapsProperties("maps-key", URI.create("https://geocode.test"),
                URI.create("https://routes.test"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        client = new GoogleMapsClient(geocodingBuilder.build(), routesBuilder.build(), properties);
    }

    @Test
    void geocodesAddressUsingHeaderCredential() {
        geocodingServer.expect(requestTo("https://geocode.test/v4/geocode/address/1600%20Pennsylvania%20Ave%20NW"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Goog-Api-Key", "maps-key"))
                .andRespond(withSuccess("""
                        {"results":[{"placeId":"place-1","formattedAddress":"1600 Pennsylvania Avenue NW, Washington, DC 20500, USA","location":{"latitude":38.8976763,"longitude":-77.0365298}}]}
                        """, MediaType.APPLICATION_JSON));

        GeocodedLocation result = client.geocode("1600 Pennsylvania Ave NW");

        assertEquals("place-1", result.placeId());
        assertEquals(new BigDecimal("38.8976763"), result.latitude());
        geocodingServer.verify();
    }

    @Test
    void rejectsAddressWithNoResults() {
        geocodingServer.expect(requestTo("https://geocode.test/v4/geocode/address/unknown"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(InvalidLocationException.class, () -> client.geocode("unknown"));
    }

    @Test
    void mapsQuotaFailureWithoutExposingResponseBody() {
        geocodingServer.expect(requestTo("https://geocode.test/v4/geocode/address/address"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        GoogleMapsServiceException exception = assertThrows(GoogleMapsServiceException.class,
                () -> client.geocode("address"));

        assertEquals(503, exception.getResponseStatus().value());
    }

    @Test
    void computesDrivingRouteWithMinimalFieldMask() {
        routesServer.expect(requestTo("https://routes.test/directions/v2:computeRoutes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "maps-key"))
                .andExpect(header("X-Goog-FieldMask",
                        "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"))
                .andRespond(withSuccess("""
                        {"routes":[{"distanceMeters":12345,"duration":"901.5s","polyline":{"encodedPolyline":"encoded"}}]}
                        """, MediaType.APPLICATION_JSON));
        GeocodedLocation origin = new GeocodedLocation("Origin", "o", new BigDecimal("42.0"), new BigDecimal("-88.0"));
        GeocodedLocation destination = new GeocodedLocation("Destination", "d", new BigDecimal("42.1"), new BigDecimal("-87.9"));

        RouteDetails result = client.computeDrivingRoute(origin, destination, Instant.parse("2027-01-01T12:00:00Z"));

        assertEquals(12345, result.distanceMeters());
        assertEquals(Duration.ofMillis(901500), result.duration());
        assertEquals("encoded", result.encodedPolyline());
        routesServer.verify();
    }

    @Test
    void reportsNoRoute() {
        routesServer.expect(requestTo("https://routes.test/directions/v2:computeRoutes"))
                .andRespond(withSuccess("{\"routes\":[]}", MediaType.APPLICATION_JSON));

        GeocodedLocation point = new GeocodedLocation("Point", "p", BigDecimal.ONE, BigDecimal.ONE);
        assertThrows(NoRouteFoundException.class,
                () -> client.computeDrivingRoute(point, point, Instant.parse("2027-01-01T12:00:00Z")));
    }
}
