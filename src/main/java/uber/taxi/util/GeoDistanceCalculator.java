package uber.taxi.util;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public double between(GeoPoint first, GeoPoint second) {
        double latitudeDelta = Math.toRadians(second.latitude() - first.latitude());
        double longitudeDelta = Math.toRadians(second.longitude() - first.longitude());
        double firstLatitude = Math.toRadians(first.latitude());
        double secondLatitude = Math.toRadians(second.latitude());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
    }

    public double distanceToRoute(GeoPoint point, List<GeoPoint> route) {
        if (route.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        if (route.size() == 1) {
            return between(point, route.getFirst());
        }
        double minimum = Double.POSITIVE_INFINITY;
        for (int index = 1; index < route.size(); index++) {
            minimum = Math.min(minimum, distanceToSegment(point, route.get(index - 1), route.get(index)));
        }
        return minimum;
    }

    private double distanceToSegment(GeoPoint point, GeoPoint start, GeoPoint end) {
        double referenceLatitude = Math.toRadians(point.latitude());
        double pointX = Math.toRadians(point.longitude()) * Math.cos(referenceLatitude) * EARTH_RADIUS_METERS;
        double pointY = Math.toRadians(point.latitude()) * EARTH_RADIUS_METERS;
        double startX = Math.toRadians(start.longitude()) * Math.cos(referenceLatitude) * EARTH_RADIUS_METERS;
        double startY = Math.toRadians(start.latitude()) * EARTH_RADIUS_METERS;
        double endX = Math.toRadians(end.longitude()) * Math.cos(referenceLatitude) * EARTH_RADIUS_METERS;
        double endY = Math.toRadians(end.latitude()) * EARTH_RADIUS_METERS;
        double segmentX = endX - startX;
        double segmentY = endY - startY;
        double lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared == 0) {
            return Math.hypot(pointX - startX, pointY - startY);
        }
        double projection = ((pointX - startX) * segmentX + (pointY - startY) * segmentY) / lengthSquared;
        double clamped = Math.clamp(projection, 0, 1);
        return Math.hypot(pointX - (startX + clamped * segmentX), pointY - (startY + clamped * segmentY));
    }
}
