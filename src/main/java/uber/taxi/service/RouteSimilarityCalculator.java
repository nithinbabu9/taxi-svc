package uber.taxi.service;

import java.util.List;
import org.springframework.stereotype.Component;
import uber.taxi.util.GeoDistanceCalculator;
import uber.taxi.util.GeoPoint;

@Component
public class RouteSimilarityCalculator {

    private static final int MAX_SAMPLED_POINTS = 200;
    private final GeoDistanceCalculator distanceCalculator;

    public RouteSimilarityCalculator(GeoDistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    public double calculate(List<GeoPoint> firstRoute, List<GeoPoint> secondRoute, double toleranceMeters) {
        if (firstRoute.isEmpty() || secondRoute.isEmpty()) {
            return 0;
        }
        double firstCoverage = coverage(sample(firstRoute), secondRoute, toleranceMeters);
        double secondCoverage = coverage(sample(secondRoute), firstRoute, toleranceMeters);
        return (firstCoverage + secondCoverage) / 2;
    }

    private double coverage(List<GeoPoint> sampledRoute, List<GeoPoint> otherRoute, double toleranceMeters) {
        long nearbyPoints = sampledRoute.stream()
                .filter(point -> distanceCalculator.distanceToRoute(point, otherRoute) <= toleranceMeters)
                .count();
        return nearbyPoints / (double) sampledRoute.size();
    }

    private List<GeoPoint> sample(List<GeoPoint> route) {
        if (route.size() <= MAX_SAMPLED_POINTS) {
            return route;
        }
        double step = (route.size() - 1.0) / (MAX_SAMPLED_POINTS - 1.0);
        return java.util.stream.IntStream.range(0, MAX_SAMPLED_POINTS)
                .mapToObj(index -> route.get((int) Math.round(index * step)))
                .toList();
    }
}
