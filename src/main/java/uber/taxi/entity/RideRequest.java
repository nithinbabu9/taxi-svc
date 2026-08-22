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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_requests", indexes = {
        @Index(name = "idx_ride_requests_user_id", columnList = "user_id"),
        @Index(name = "idx_ride_requests_status_departure", columnList = "status,departure_time")
})
public class RideRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String pickupAddress;

    @Column(precision = 9, scale = 6)
    private BigDecimal pickupLatitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal pickupLongitude;

    @Column
    private String pickupPlaceId;

    @Column(nullable = false, length = 500)
    private String destinationAddress;

    @Column(precision = 9, scale = 6)
    private BigDecimal destinationLatitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal destinationLongitude;

    @Column
    private String destinationPlaceId;

    private Long routeDistanceMeters;

    private Long routeDurationSeconds;

    @Column(length = 20000)
    private String routeEncodedPolyline;

    @Column(nullable = false)
    private Instant departureTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideRequestStatus status;

    @SuppressWarnings("unused") // Required by JPA.
    protected RideRequest() {
    }

    public RideRequest(User user, String pickupAddress, String destinationAddress, Instant departureTime) {
        this.user = user;
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
        this.departureTime = departureTime;
        this.status = RideRequestStatus.OPEN;
    }

    public void update(String pickupAddress, String destinationAddress, Instant departureTime) {
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
        this.departureTime = departureTime;
        clearResolvedLocations();
    }

    public void cancel() {
        status = RideRequestStatus.CANCELLED;
    }

    public void markMatched() {
        if (status != RideRequestStatus.OPEN) {
            throw new IllegalStateException("Only an OPEN ride can be matched");
        }
        status = RideRequestStatus.MATCHED;
    }

    public void applyResolvedRoute(String pickupAddress, BigDecimal pickupLatitude, BigDecimal pickupLongitude,
                                   String pickupPlaceId, String destinationAddress,
                                   BigDecimal destinationLatitude, BigDecimal destinationLongitude,
                                   String destinationPlaceId, long routeDistanceMeters,
                                   long routeDurationSeconds, String routeEncodedPolyline) {
        this.pickupAddress = pickupAddress;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.pickupPlaceId = pickupPlaceId;
        this.destinationAddress = destinationAddress;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.destinationPlaceId = destinationPlaceId;
        this.routeDistanceMeters = routeDistanceMeters;
        this.routeDurationSeconds = routeDurationSeconds;
        this.routeEncodedPolyline = routeEncodedPolyline;
    }

    private void clearResolvedLocations() {
        pickupLatitude = null;
        pickupLongitude = null;
        pickupPlaceId = null;
        destinationLatitude = null;
        destinationLongitude = null;
        destinationPlaceId = null;
        routeDistanceMeters = null;
        routeDurationSeconds = null;
        routeEncodedPolyline = null;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getPickupAddress() { return pickupAddress; }
    public BigDecimal getPickupLatitude() { return pickupLatitude; }
    public BigDecimal getPickupLongitude() { return pickupLongitude; }
    public String getPickupPlaceId() { return pickupPlaceId; }
    public String getDestinationAddress() { return destinationAddress; }
    public BigDecimal getDestinationLatitude() { return destinationLatitude; }
    public BigDecimal getDestinationLongitude() { return destinationLongitude; }
    public String getDestinationPlaceId() { return destinationPlaceId; }
    public Long getRouteDistanceMeters() { return routeDistanceMeters; }
    public Long getRouteDurationSeconds() { return routeDurationSeconds; }
    public String getRouteEncodedPolyline() { return routeEncodedPolyline; }
    public Instant getDepartureTime() { return departureTime; }
    public RideRequestStatus getStatus() { return status; }
}
