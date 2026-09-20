package uber.taxi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import uber.taxi.validation.GmailAddress;

public record LoginRequest(@NotBlank @Email @GmailAddress String email, @NotBlank String password) { }
