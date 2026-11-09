package uber.taxi.controller;

import jakarta.validation.constraints.Size;
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
}
