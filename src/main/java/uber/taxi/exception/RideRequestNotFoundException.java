package uber.taxi.exception;

import java.util.UUID;

public class RideRequestNotFoundException extends ResourceNotFoundException {
    public RideRequestNotFoundException(UUID id) {
        super("Ride request not found: " + id);
    }
}
