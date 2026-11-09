package uber.taxi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uber.taxi.validation.GmailAddress;

public record ResetPasswordRequest(
        @NotBlank @GmailAddress String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a six-digit code") String code,
        @NotBlank @Size(min = 12, max = 72, message = "must be between 12 and 72 characters") String newPassword) {
}
