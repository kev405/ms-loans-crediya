package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;
import java.math.BigDecimal;
import java.util.Objects;

public record InterestRate(BigDecimal annualPercent) {
    public InterestRate {
        Objects.requireNonNull(annualPercent, "interest rate is required");
        if (annualPercent.compareTo(BigDecimal.ZERO) < 0)
            throw new DomainValidationException("INVALID_INTEREST","interest rate must be >= 0");
    }
}
