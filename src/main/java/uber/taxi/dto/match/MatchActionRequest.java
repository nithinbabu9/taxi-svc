package uber.taxi.dto.match;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identifies which matched user is accepting or rejecting")
public record MatchActionRequest(
        @Schema(example = "3d4692be-cb4d-439d-ba31-5088fa217cd6") @NotNull UUID userId) { }
