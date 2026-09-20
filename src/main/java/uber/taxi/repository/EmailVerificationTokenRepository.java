package uber.taxi.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uber.taxi.entity.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
}
