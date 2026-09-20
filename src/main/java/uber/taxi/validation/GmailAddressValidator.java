package uber.taxi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class GmailAddressValidator implements ConstraintValidator<GmailAddress, String> {
    private static final Pattern GMAIL_ADDRESS = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9._+\\-]{0,62}[a-z0-9])?@gmail\\.com$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || GMAIL_ADDRESS.matcher(value.trim()).matches();
    }
}
