package uber.taxi.dto.auth;

import java.time.Instant;
import uber.taxi.dto.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) { }
