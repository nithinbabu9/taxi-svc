package uber.taxi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import uber.taxi.util.GeoDistanceCalculator;
import uber.taxi.util.GeoPoint;

class RouteSimilarityCalculatorTest {

    private final RouteSimilarityCalculator calculator =
            new RouteSimilarityCalculator(new GeoDistanceCalculator());

    @Test
    void identicalRoutesHaveCompleteOverlap() {
        List<GeoPoint> route = List.of(new GeoPoint(42.0, -88.0), new GeoPoint(42.1, -87.9));
        assertEquals(1.0, calculator.calculate(route, route, 100), 0.0001);
    }

    @Test
    void separatedRoutesHaveNoOverlap() {
        List<GeoPoint> first = List.of(new GeoPoint(42.0, -88.0), new GeoPoint(42.1, -87.9));
        List<GeoPoint> second = List.of(new GeoPoint(43.0, -88.0), new GeoPoint(43.1, -87.9));
        assertTrue(calculator.calculate(first, second, 1_000) < 0.01);
    }
}
