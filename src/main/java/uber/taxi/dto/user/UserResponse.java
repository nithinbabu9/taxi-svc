package uber.taxi.dto.user;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered platform user")
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
