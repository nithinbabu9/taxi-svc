package uber.taxi.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uber.taxi.client.MapsClient;
import uber.taxi.dto.location.LocationSuggestionResponse;

@RestController
@RequestMapping("/api/locations")
@Validated
public class LocationController {
    private final MapsClient mapsClient;

    public LocationController(MapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @GetMapping("/autocomplete")
    public List<LocationSuggestionResponse> autocomplete(
            @RequestParam @Size(min = 3, max = 200) String input) {
        return mapsClient.autocomplete(input.strip()).stream()
                .map(suggestion -> new LocationSuggestionResponse(suggestion.placeId(), suggestion.description()))
                .toList();
    }

    @GetMapping("/current")
    public LocationSuggestionResponse currentLocation(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude) {
        var location = mapsClient.reverseGeocode(latitude, longitude);
        return new LocationSuggestionResponse(location.placeId(), location.formattedAddress());
    }
}
