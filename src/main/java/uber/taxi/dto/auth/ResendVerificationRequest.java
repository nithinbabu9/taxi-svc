package uber.taxi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import uber.taxi.validation.GmailAddress;

public record ResendVerificationRequest(@NotBlank @GmailAddress String email) {
}
