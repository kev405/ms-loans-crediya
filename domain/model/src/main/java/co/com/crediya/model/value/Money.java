package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal value) {
    public Money {
        Objects.requireNonNull(value, "amount is required");
        if (value.signum() < 0) throw new DomainValidationException("INVALID_QUANTITY_MONEY","amount must be >= 0");
        value = value.setScale(2, RoundingMode.HALF_UP);
    }
}
