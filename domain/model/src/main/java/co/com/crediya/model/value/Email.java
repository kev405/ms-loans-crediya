package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;
import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {
    private static final Pattern P = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    public Email {
        Objects.requireNonNull(value, "email is required");
        if (!P.matcher(value).matches()) throw new DomainValidationException("INVALID_EMAIL","invalid email format");
    }
}
