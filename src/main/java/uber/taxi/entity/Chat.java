package uber.taxi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "chats", uniqueConstraints =
        @UniqueConstraint(name = "uk_chats_ride_match", columnNames = "ride_match_id"))
public class Chat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_match_id", nullable = false, unique = true)
    private RideMatch rideMatch;

    protected Chat() { }

    public Chat(RideMatch rideMatch) {
        this.rideMatch = rideMatch;
    }

    public UUID getId() { return id; }
    public RideMatch getRideMatch() { return rideMatch; }
}
