package uber.taxi.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Message submitted by a participant in an accepted-match chat")
public record SendMessageRequest(
        @Schema(example = "See you at the pickup point", maxLength = 4000) @NotBlank @Size(max = 4000) String message
) { }
