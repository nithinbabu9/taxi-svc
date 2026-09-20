package uber.taxi.service;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uber.taxi.config.JwtProperties;
import uber.taxi.config.EmailVerificationProperties;
import uber.taxi.dto.auth.AuthResponse;
import uber.taxi.dto.auth.LoginRequest;
import uber.taxi.dto.auth.RegisterRequest;
import uber.taxi.dto.auth.VerificationCodeRequest;
import uber.taxi.dto.auth.VerificationPendingResponse;
import uber.taxi.entity.EmailVerificationToken;
import uber.taxi.entity.User;
import uber.taxi.exception.ConflictException;
import uber.taxi.exception.ForbiddenException;
import uber.taxi.exception.InvalidRequestException;
import uber.taxi.mapper.UserMapper;
import uber.taxi.repository.UserRepository;
import uber.taxi.repository.EmailVerificationTokenRepository;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final UserMapper userMapper;
    private final EmailVerificationProperties emailProperties;
    private final EmailVerificationSender emailSender;

    public AuthService(UserRepository users, EmailVerificationTokenRepository verificationTokens,
                       PasswordEncoder passwords, JwtEncoder jwtEncoder, JwtProperties properties,
                       EmailVerificationProperties emailProperties, EmailVerificationSender emailSender,
                       UserMapper userMapper) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.emailProperties = emailProperties;
        this.emailSender = emailSender;
        this.userMapper = userMapper;
    }

    @Transactional
    public VerificationPendingResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email already exists");
        }
        String phone = request.phoneNumber() == null || request.phoneNumber().isBlank()
                ? null : request.phoneNumber().trim();
        User user = users.save(new User(request.firstName().trim(), request.lastName().trim(), email, phone,
                passwords.encode(request.password())));
        return issueVerificationCode(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(() -> new InvalidRequestException("Invalid email or password"));
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidRequestException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Verify your Gmail address before logging in");
        }
        return tokenFor(user);
    }

    @Transactional
    public AuthResponse verifyEmail(VerificationCodeRequest request) {
        User user = users.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(() -> new InvalidRequestException("Invalid verification code"));
        if (user.isEmailVerified()) {
            return tokenFor(user);
        }
        EmailVerificationToken token = verificationTokens.findById(user.getId())
                .orElseThrow(() -> new InvalidRequestException("Verification code has expired. Request a new code."));
        if (Instant.now().isAfter(token.getExpiresAt())) {
            verificationTokens.delete(token);
            throw new InvalidRequestException("Verification code has expired. Request a new code.");
        }
        if (token.getAttempts() >= emailProperties.maximumAttempts()) {
            throw new InvalidRequestException("Too many incorrect attempts. Request a new code.");
        }
        if (!passwords.matches(request.code(), token.getCodeHash())) {
            token.recordFailedAttempt();
            verificationTokens.save(token);
            throw new InvalidRequestException("Invalid verification code");
        }
        user.markEmailVerified();
        users.save(user);
        verificationTokens.delete(token);
        return tokenFor(user);
    }

    @Transactional
    public VerificationPendingResponse resendVerification(String rawEmail) {
        User user = users.findByEmailIgnoreCase(normalize(rawEmail))
                .orElseThrow(() -> new InvalidRequestException("No pending verification exists for this email"));
        if (user.isEmailVerified()) {
            throw new InvalidRequestException("This Gmail address is already verified");
        }
        verificationTokens.findById(user.getId()).ifPresent(token -> {
            if (Instant.now().isBefore(token.getSentAt().plus(emailProperties.resendCooldown()))) {
                throw new InvalidRequestException("Please wait before requesting another verification code");
            }
        });
        return issueVerificationCode(user);
    }

    private VerificationPendingResponse issueVerificationCode(User user) {
        Instant sentAt = Instant.now();
        Instant expiresAt = sentAt.plus(emailProperties.codeLifetime());
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String codeHash = passwords.encode(code);
        verificationTokens.findById(user.getId()).ifPresentOrElse(
                token -> token.replaceCode(codeHash, expiresAt, sentAt),
                () -> verificationTokens.save(new EmailVerificationToken(user.getId(), codeHash, expiresAt, sentAt)));
        emailSender.sendVerificationCode(user.getEmail(), code, expiresAt);
        return new VerificationPendingResponse(user.getEmail(), expiresAt, true);
    }

    private AuthResponse tokenFor(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.lifetime());
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("taxi-api").issuedAt(issuedAt).expiresAt(expiresAt)
                .subject(user.getId().toString()).claim("email", user.getEmail()).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expiresAt, userMapper.toResponse(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
