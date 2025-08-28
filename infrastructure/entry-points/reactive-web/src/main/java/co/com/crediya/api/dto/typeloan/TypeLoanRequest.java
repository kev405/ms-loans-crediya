package co.com.crediya.api.dto.typeloan;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TypeLoanRequest(
        @NotBlank String name,
        @NotNull  @DecimalMin("0.00") BigDecimal minimumAmount,
        @NotNull  @DecimalMin("0.00") BigDecimal maximumAmount,
        @NotNull  @DecimalMin("0.00") BigDecimal annualInterestPercent,
        boolean automaticValidation
) {}
