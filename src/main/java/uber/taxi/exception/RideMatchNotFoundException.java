package uber.taxi.exception;

import java.util.UUID;

public class RideMatchNotFoundException extends ResourceNotFoundException {
    public RideMatchNotFoundException(UUID id) {
        super("Ride match not found: " + id);
    }
}
