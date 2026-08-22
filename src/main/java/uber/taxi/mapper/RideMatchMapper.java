package uber.taxi.mapper;

import org.springframework.stereotype.Component;
import uber.taxi.dto.match.RideMatchResponse;
import uber.taxi.entity.RideMatch;

@Component
public class RideMatchMapper {
    public RideMatchResponse toResponse(RideMatch match) {
        return new RideMatchResponse(match.getId(), match.getRideRequest1().getId(), match.getRideRequest2().getId(),
                match.getMatchScore(), match.getPickupDistanceMeters(), match.getDestinationDistanceMeters(),
                match.getRouteSimilarity(), match.getDepartureTimeDifferenceSeconds(), match.getStatus(),
                match.getRideRequest1AcceptedAt(), match.getRideRequest2AcceptedAt(), match.getCreatedAt());
    }
}
