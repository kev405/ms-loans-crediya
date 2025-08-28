package co.com.crediya.api.dto.typeloan;

import java.math.BigDecimal;

public record TypeLoanResponse(
        String id,
        String name,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        BigDecimal annualInterestPercent,
        boolean automaticValidation
) {}
