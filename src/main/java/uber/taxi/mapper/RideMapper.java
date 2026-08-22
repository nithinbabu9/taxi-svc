package uber.taxi.mapper;

import org.springframework.stereotype.Component;
import uber.taxi.dto.ride.RideResponse;
import uber.taxi.entity.RideRequest;

@Component
public class RideMapper {

    public RideResponse toResponse(RideRequest ride) {
        return new RideResponse(ride.getId(), ride.getUser().getId(), ride.getPickupAddress(),
                ride.getPickupLatitude(), ride.getPickupLongitude(), ride.getPickupPlaceId(), ride.getDestinationAddress(),
                ride.getDestinationLatitude(), ride.getDestinationLongitude(), ride.getDestinationPlaceId(),
                ride.getRouteDistanceMeters(), ride.getRouteDurationSeconds(), ride.getRouteEncodedPolyline(), ride.getDepartureTime(),
                ride.getStatus(), ride.getCreatedAt(), ride.getUpdatedAt());
    }
}
