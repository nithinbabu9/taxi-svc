package uber.taxi.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uber.taxi.dto.auth.AuthResponse;
import uber.taxi.dto.auth.LoginRequest;
import uber.taxi.dto.auth.RegisterRequest;
import uber.taxi.dto.auth.ResendVerificationRequest;
import uber.taxi.dto.auth.VerificationCodeRequest;
import uber.taxi.dto.auth.VerificationPendingResponse;
import uber.taxi.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@SecurityRequirements
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Register a traveler", description = "Creates an unverified account and emails a six-digit verification code. Verify it before logging in.")
    @ApiResponses({@ApiResponse(responseCode = "202", description = "Verification code sent"), @ApiResponse(responseCode = "400", description = "Validation failed"), @ApiResponse(responseCode = "409", description = "Email is already registered")})
    public VerificationPendingResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify a Gmail address", description = "Confirms the six-digit code and returns a JWT bearer token.")
    public AuthResponse verifyEmail(@Valid @RequestBody VerificationCodeRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Resend an email verification code")
    public VerificationPendingResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return authService.resendVerification(request.email());
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Verifies email and password and returns a JWT bearer token.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Logged in"), @ApiResponse(responseCode = "401", description = "Invalid email or password")})
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
