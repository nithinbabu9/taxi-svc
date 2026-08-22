package uber.taxi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information required to register an MVP user")
public record CreateUserRequest(
        @Schema(example = "Nithin", maxLength = 100) @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "User", maxLength = 100) @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "nithin@example.com", maxLength = 320) @NotBlank @Email @Size(max = 320) String email,
        @Schema(example = "+1 312-555-0101", nullable = true) @Pattern(regexp = "^$|^[+0-9(). -]{7,30}$", message = "must be a valid phone number") String phoneNumber
) {
}
