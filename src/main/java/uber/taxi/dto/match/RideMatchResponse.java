package uber.taxi.dto.match;

import java.time.Instant;
import java.util.UUID;
import uber.taxi.entity.RideMatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compatibility evaluation for two ride requests. Scores range from 0 to 1; distances are meters.")
public record RideMatchResponse(
        UUID id,
        UUID rideRequest1Id,
        UUID rideRequest2Id,
        double matchScore,
        double pickupDistanceMeters,
        double destinationDistanceMeters,
        double routeSimilarity,
        long departureTimeDifferenceSeconds,
        RideMatchStatus status,
        Instant rideRequest1AcceptedAt,
        Instant rideRequest2AcceptedAt,
        Instant createdAt
) { }
