package uber.taxi.dto.ride;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Replacement trip details for an existing OPEN ride")
public record UpdateRideRequest(
        @Schema(example = "Arlington Heights, IL") @NotBlank @Size(max = 500) String pickupAddress,
        @Schema(example = "Millennium Park, Chicago, IL") @NotBlank @Size(max = 500) String destinationAddress,
        @Schema(example = "2030-08-17T15:50:28.760Z") @NotNull @Future Instant departureTime
) {
}
