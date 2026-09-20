package uber.taxi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import uber.taxi.validation.GmailAddress;

public record VerificationCodeRequest(
        @NotBlank @GmailAddress String email,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "must be a six-digit code") String code
) {
}
