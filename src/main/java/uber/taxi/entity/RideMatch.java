package uber.taxi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "ride_matches",
        uniqueConstraints = @UniqueConstraint(name = "uk_ride_matches_pair",
                columnNames = {"ride_request_1_id", "ride_request_2_id"}),
        indexes = {
                @Index(name = "idx_ride_matches_ride_1", columnList = "ride_request_1_id"),
                @Index(name = "idx_ride_matches_ride_2", columnList = "ride_request_2_id"),
                @Index(name = "idx_ride_matches_status", columnList = "status")
        })
public class RideMatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_request_1_id", nullable = false)
    private RideRequest rideRequest1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_request_2_id", nullable = false)
    private RideRequest rideRequest2;

    @Column(nullable = false)
    private double matchScore;

    @Column(nullable = false)
    private double pickupDistanceMeters;

    @Column(nullable = false)
    private double destinationDistanceMeters;

    @Column(nullable = false)
    private double routeSimilarity;

    @Column(nullable = false)
    private long departureTimeDifferenceSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideMatchStatus status;

    @Column(name = "ride_request_1_accepted_at")
    private Instant rideRequest1AcceptedAt;

    @Column(name = "ride_request_2_accepted_at")
    private Instant rideRequest2AcceptedAt;

    @SuppressWarnings("unused") // Required by JPA.
    protected RideMatch() {
    }

    public RideMatch(RideRequest rideRequest1, RideRequest rideRequest2, double matchScore,
                     double pickupDistanceMeters, double destinationDistanceMeters,
                     double routeSimilarity, long departureTimeDifferenceSeconds) {
        if (rideRequest1.getId().compareTo(rideRequest2.getId()) >= 0) {
            throw new IllegalArgumentException("Ride matches must use canonical ride ID ordering");
        }
        this.rideRequest1 = rideRequest1;
        this.rideRequest2 = rideRequest2;
        this.matchScore = matchScore;
        this.pickupDistanceMeters = pickupDistanceMeters;
        this.destinationDistanceMeters = destinationDistanceMeters;
        this.routeSimilarity = routeSimilarity;
        this.departureTimeDifferenceSeconds = departureTimeDifferenceSeconds;
        this.status = RideMatchStatus.PENDING;
    }

    public void refresh(double matchScore, double pickupDistanceMeters, double destinationDistanceMeters,
                        double routeSimilarity, long departureTimeDifferenceSeconds) {
        this.matchScore = matchScore;
        this.pickupDistanceMeters = pickupDistanceMeters;
        this.destinationDistanceMeters = destinationDistanceMeters;
        this.routeSimilarity = routeSimilarity;
        this.departureTimeDifferenceSeconds = departureTimeDifferenceSeconds;
        this.status = RideMatchStatus.PENDING;
    }

    public boolean accept(UUID userId, Instant acceptedAt) {
        requirePendingOrAccepted();
        if (rideRequest1.getUser().getId().equals(userId)) {
            if (rideRequest1AcceptedAt == null) {
                rideRequest1AcceptedAt = acceptedAt;
            }
        } else if (rideRequest2.getUser().getId().equals(userId)) {
            if (rideRequest2AcceptedAt == null) {
                rideRequest2AcceptedAt = acceptedAt;
            }
        } else {
            throw new IllegalArgumentException("User is not a participant in this match");
        }
        if (rideRequest1AcceptedAt != null && rideRequest2AcceptedAt != null) {
            status = RideMatchStatus.ACCEPTED;
            return true;
        }
        return false;
    }

    public void reject(UUID userId) {
        if (status != RideMatchStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING match can be rejected");
        }
        if (!rideRequest1.getUser().getId().equals(userId)
                && !rideRequest2.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("User is not a participant in this match");
        }
        status = RideMatchStatus.REJECTED;
    }

    private void requirePendingOrAccepted() {
        if (status != RideMatchStatus.PENDING && status != RideMatchStatus.ACCEPTED) {
            throw new IllegalStateException("Only a PENDING match can be accepted");
        }
    }

    public UUID getId() { return id; }
    public RideRequest getRideRequest1() { return rideRequest1; }
    public RideRequest getRideRequest2() { return rideRequest2; }
    public double getMatchScore() { return matchScore; }
    public double getPickupDistanceMeters() { return pickupDistanceMeters; }
    public double getDestinationDistanceMeters() { return destinationDistanceMeters; }
    public double getRouteSimilarity() { return routeSimilarity; }
    public long getDepartureTimeDifferenceSeconds() { return departureTimeDifferenceSeconds; }
    public RideMatchStatus getStatus() { return status; }
    public Instant getRideRequest1AcceptedAt() { return rideRequest1AcceptedAt; }
    public Instant getRideRequest2AcceptedAt() { return rideRequest2AcceptedAt; }
}
