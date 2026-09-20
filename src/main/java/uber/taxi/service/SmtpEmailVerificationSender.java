package uber.taxi.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import uber.taxi.config.EmailVerificationProperties;
import uber.taxi.exception.EmailDeliveryException;

@Service
@Profile("!local")
public class SmtpEmailVerificationSender implements EmailVerificationSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailVerificationSender.class);
    private final JavaMailSender mailSender;
    private final EmailVerificationProperties properties;

    public SmtpEmailVerificationSender(JavaMailSender mailSender, EmailVerificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(String recipient, String code, Instant expiresAt) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(recipient);
            message.setSubject("Your Wayfare verification code");
            message.setText("Your Wayfare verification code is " + code + ". It expires at " + expiresAt
                    + ". If you did not create this account, you can ignore this email.");
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.warn("SMTP verification email delivery failed: {}", exception.getMessage());
            throw new EmailDeliveryException("Unable to send the verification email", exception);
        }
    }
}
