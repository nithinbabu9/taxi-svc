package uber.taxi.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uber.taxi.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
}
