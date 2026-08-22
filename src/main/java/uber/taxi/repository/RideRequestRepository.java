package uber.taxi.repository;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uber.taxi.entity.RideRequestStatus;
import uber.taxi.entity.RideRequest;

public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {
    List<RideRequest> findByUserIdOrderByDepartureTimeDesc(UUID userId);

    @Query("""
            select ride from RideRequest ride
            join fetch ride.user user
            where ride.status = :status
              and ride.id <> :rideId
              and user.id <> :userId
              and ride.departureTime between :from and :to
            """)
    List<RideRequest> findMatchingCandidates(
            @Param("rideId") UUID rideId,
            @Param("userId") UUID userId,
            @Param("status") RideRequestStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
