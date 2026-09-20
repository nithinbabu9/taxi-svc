package uber.taxi.dto.ride;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A user's planned trip. Addresses are resolved by the configured map provider.")
public record CreateRideRequest(
        @Schema(example = "Schaumburg, IL") @NotBlank @Size(max = 500) String pickupAddress,
        @Schema(example = "Millennium Park, Chicago, IL") @NotBlank @Size(max = 500) String destinationAddress,
        @Schema(description = "Future UTC instant", example = "2030-08-17T15:40:28.760Z") @NotNull @Future Instant departureTime
) {
}
