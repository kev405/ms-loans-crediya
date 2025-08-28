package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;

public record TermMonths(Integer value) {
    public TermMonths {
        if (value == null || value < 1) throw new DomainValidationException("INVALID_TERM_MONTHS","term must be >= 1 month");
    }
}
