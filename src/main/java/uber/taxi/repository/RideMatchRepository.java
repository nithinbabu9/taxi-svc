package uber.taxi.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import uber.taxi.entity.RideMatchStatus;
import uber.taxi.entity.RideMatch;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface RideMatchRepository extends JpaRepository<RideMatch, UUID> {

    Optional<RideMatch> findByRideRequest1IdAndRideRequest2Id(UUID rideRequest1Id, UUID rideRequest2Id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rm from RideMatch rm
            join fetch rm.rideRequest1 ride1
            join fetch ride1.user
            join fetch rm.rideRequest2 ride2
            join fetch ride2.user
            where rm.id = :matchId
            """)
    Optional<RideMatch> findByIdForUpdate(@Param("matchId") UUID matchId);

    @Modifying
    @Query("""
            update RideMatch rm set rm.status = :cancelled
            where rm.status = :pending
              and (rm.rideRequest1.id = :rideId or rm.rideRequest2.id = :rideId)
            """)
    void cancelPendingForRide(@Param("rideId") UUID rideId,
                              @Param("pending") RideMatchStatus pending,
                              @Param("cancelled") RideMatchStatus cancelled);

    @Query("""
            select rm from RideMatch rm
            join fetch rm.rideRequest1 ride1
            join fetch ride1.user
            join fetch rm.rideRequest2 ride2
            join fetch ride2.user
            where ride1.id = :rideId or ride2.id = :rideId
            order by rm.matchScore desc
            """)
    List<RideMatch> findAllForRide(@Param("rideId") UUID rideId);

    @Query("""
            select rm from RideMatch rm
            join fetch rm.rideRequest1 ride1
            join fetch ride1.user
            join fetch rm.rideRequest2 ride2
            join fetch ride2.user
            where ride1.user.id = :userId or ride2.user.id = :userId
            order by rm.matchScore desc
            """)
    List<RideMatch> findAllForUser(@Param("userId") UUID userId);
}
