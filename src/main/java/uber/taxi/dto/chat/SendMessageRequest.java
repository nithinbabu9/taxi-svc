package uber.taxi.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Message submitted by a participant in an accepted-match chat")
public record SendMessageRequest(
        @Schema(example = "3d4692be-cb4d-439d-ba31-5088fa217cd6") @NotNull UUID senderId,
        @Schema(example = "See you at the pickup point", maxLength = 4000) @NotBlank @Size(max = 4000) String message
) { }
