package uber.taxi.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import uber.taxi.config.GoogleMapsProperties;
import uber.taxi.exception.GoogleMapsServiceException;
import uber.taxi.exception.InvalidLocationException;
import uber.taxi.exception.NoRouteFoundException;

@Component
@Profile("!local")
public class GoogleMapsClient implements MapsClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsClient.class);
    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String ROUTE_FIELD_MASK =
            "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline";
    private static final String AUTOCOMPLETE_FIELD_MASK =
            "suggestions.placePrediction.placeId,suggestions.placePrediction.text.text";

    private final RestClient geocodingClient;
    private final RestClient routesClient;
    private final RestClient placesClient;
    private final GoogleMapsProperties properties;

    public GoogleMapsClient(
            @Qualifier("googleGeocodingRestClient") RestClient geocodingClient,
            @Qualifier("googleRoutesRestClient") RestClient routesClient,
            @Qualifier("googlePlacesRestClient") RestClient placesClient,
            GoogleMapsProperties properties) {
        this.geocodingClient = geocodingClient;
        this.routesClient = routesClient;
        this.placesClient = placesClient;
        this.properties = properties;
    }

    @Override
    public List<PlaceSuggestion> autocomplete(String input) {
        try {
            AutocompleteResponse response = placesClient.post()
                    .uri("/v1/places:autocomplete")
                    .header(API_KEY_HEADER, properties.apiKey())
                    .header("X-Goog-FieldMask", AUTOCOMPLETE_FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new AutocompleteRequest(input, "US", "en"))
                    .retrieve()
                    .body(AutocompleteResponse.class);
            if (response == null || response.suggestions() == null) {
                return List.of();
            }
            return response.suggestions().stream()
                    .map(AutocompleteSuggestion::placePrediction)
                    .filter(prediction -> prediction != null && prediction.placeId() != null
                            && prediction.text() != null && prediction.text().text() != null)
                    .map(prediction -> new PlaceSuggestion(prediction.placeId(), prediction.text().text()))
                    .limit(5)
                    .toList();
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure("address autocomplete", exception);
        } catch (ResourceAccessException exception) {
            throw unavailable("Google address autocomplete timed out or could not be reached", exception);
        } catch (RestClientException exception) {
            throw malformed("Google returned a malformed address autocomplete response", exception);
        }
    }

    @Override
    public GeocodedLocation geocode(String address) {
        try {
            GeocodeResponse response = geocodingClient.get()
                    .uri(uriBuilder -> uriBuilder.pathSegment("v4", "geocode", "address", address).build())
                    .header(API_KEY_HEADER, properties.apiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GeocodeResponse.class);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw new InvalidLocationException("No location found for the supplied address");
            }
            GeocodeResult result = response.results().getFirst();
            if (result.location() == null || result.formattedAddress() == null) {
                throw new InvalidLocationException("Google returned an incomplete location result");
            }
            return new GeocodedLocation(result.formattedAddress(), result.placeId(),
                    result.location().latitude(), result.location().longitude());
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure("geocoding", exception);
        } catch (ResourceAccessException exception) {
            throw unavailable("Google geocoding timed out or could not be reached", exception);
        } catch (RestClientException exception) {
            throw malformed("Google returned a malformed geocoding response", exception);
        }
    }

    @Override
    public RouteDetails computeDrivingRoute(GeocodedLocation origin, GeocodedLocation destination,
                                             Instant departureTime) {
        RouteRequest request = new RouteRequest(
                waypoint(origin), waypoint(destination), "DRIVE", "TRAFFIC_AWARE",
                false, departureTime.toString(), "en-US", "METRIC");
        try {
            RoutesResponse response = routesClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .header(API_KEY_HEADER, properties.apiKey())
                    .header("X-Goog-FieldMask", ROUTE_FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RoutesResponse.class);
            if (response == null || response.routes() == null || response.routes().isEmpty()) {
                throw new NoRouteFoundException("No driving route was found between pickup and destination");
            }
            RouteResult route = response.routes().getFirst();
            if (route.distanceMeters() == null || route.duration() == null) {
                throw new NoRouteFoundException("Google returned an incomplete route result");
            }
            String polyline = route.polyline() == null ? null : route.polyline().encodedPolyline();
            return new RouteDetails(route.distanceMeters(), parseGoogleDuration(route.duration()), polyline);
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure("routing", exception);
        } catch (ResourceAccessException exception) {
            throw unavailable("Google routing timed out or could not be reached", exception);
        } catch (RestClientException exception) {
            throw malformed("Google returned a malformed routing response", exception);
        }
    }

    private Waypoint waypoint(GeocodedLocation location) {
        return new Waypoint(new Location(new LatLng(location.latitude(), location.longitude())));
    }

    private Duration parseGoogleDuration(String value) {
        if (!value.endsWith("s")) {
            throw new GoogleMapsServiceException("Google returned an invalid route duration",
                    HttpStatus.BAD_GATEWAY, null);
        }
        try {
            BigDecimal seconds = new BigDecimal(value.substring(0, value.length() - 1));
            long wholeSeconds = seconds.longValue();
            int nanos = seconds.subtract(BigDecimal.valueOf(wholeSeconds))
                    .movePointRight(9).setScale(0, RoundingMode.HALF_UP).intValueExact();
            return Duration.ofSeconds(wholeSeconds, nanos);
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new GoogleMapsServiceException("Google returned an invalid route duration",
                    HttpStatus.BAD_GATEWAY, exception);
        }
    }

    private GoogleMapsServiceException mapHttpFailure(String operation, RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        HttpStatus outwardStatus = status == 429 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        log.warn("Google Maps {} call returned HTTP {}", operation, status);
        String message = status == 429
                ? "Google Maps quota is temporarily unavailable"
                : "Google Maps " + operation + " request failed";
        return new GoogleMapsServiceException(message, outwardStatus, exception);
    }

    private GoogleMapsServiceException unavailable(String message, Exception cause) {
        log.warn(message);
        return new GoogleMapsServiceException(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }

    private GoogleMapsServiceException malformed(String message, Exception cause) {
        log.warn(message);
        return new GoogleMapsServiceException(message, HttpStatus.BAD_GATEWAY, cause);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodeResponse(List<GeocodeResult> results) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeocodeResult(String placeId, String formattedAddress, LatLng location) { }

    record AutocompleteRequest(String input, String regionCode, String languageCode) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AutocompleteResponse(List<AutocompleteSuggestion> suggestions) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AutocompleteSuggestion(PlacePrediction placePrediction) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlacePrediction(String placeId, TextValue text) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TextValue(String text) { }

    record RouteRequest(Waypoint origin, Waypoint destination, String travelMode, String routingPreference,
                        boolean computeAlternativeRoutes, String departureTime, String languageCode, String units) { }

    record Waypoint(Location location) { }
    record Location(LatLng latLng) { }
    record LatLng(BigDecimal latitude, BigDecimal longitude) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoutesResponse(List<RouteResult> routes) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RouteResult(Long distanceMeters, String duration, Polyline polyline) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Polyline(String encodedPolyline) { }
}
