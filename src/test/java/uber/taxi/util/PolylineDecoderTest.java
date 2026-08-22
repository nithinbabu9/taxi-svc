package uber.taxi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PolylineDecoderTest {

    private final PolylineDecoder decoder = new PolylineDecoder();

    @Test
    void decodesGoogleReferencePolyline() {
        List<GeoPoint> points = decoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@");

        assertEquals(3, points.size());
        assertEquals(38.5, points.getFirst().latitude(), 0.00001);
        assertEquals(-120.2, points.getFirst().longitude(), 0.00001);
        assertEquals(43.252, points.getLast().latitude(), 0.00001);
        assertEquals(-126.453, points.getLast().longitude(), 0.00001);
    }

    @Test
    void rejectsMalformedPolyline() {
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("_p~iF"));
    }
}
