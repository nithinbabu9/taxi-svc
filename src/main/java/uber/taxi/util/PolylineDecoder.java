package uber.taxi.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PolylineDecoder {

    public List<GeoPoint> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        List<GeoPoint> points = new ArrayList<>();
        int index = 0;
        int latitude = 0;
        int longitude = 0;
        while (index < encoded.length()) {
            DecodedValue latitudeValue = decodeValue(encoded, index);
            index = latitudeValue.nextIndex();
            latitude += latitudeValue.value();
            DecodedValue longitudeValue = decodeValue(encoded, index);
            index = longitudeValue.nextIndex();
            longitude += longitudeValue.value();
            points.add(new GeoPoint(latitude / 100000.0, longitude / 100000.0));
        }
        return List.copyOf(points);
    }

    private DecodedValue decodeValue(String encoded, int startIndex) {
        int result = 0;
        int shift = 0;
        int index = startIndex;
        int value;
        do {
            if (index >= encoded.length() || shift > 30) {
                throw new IllegalArgumentException("Malformed encoded polyline");
            }
            value = encoded.charAt(index++) - 63;
            result |= (value & 0x1f) << shift;
            shift += 5;
        } while (value >= 0x20);
        int decoded = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
        return new DecodedValue(decoded, index);
    }

    private record DecodedValue(int value, int nextIndex) { }
}
