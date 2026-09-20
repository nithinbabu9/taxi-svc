package uber.taxi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import uber.taxi.validation.GmailAddress;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @GmailAddress @Size(max = 320) String email,
        @Size(max = 30) String phoneNumber,
        @NotBlank @Size(min = 12, max = 72) String password
) { }
