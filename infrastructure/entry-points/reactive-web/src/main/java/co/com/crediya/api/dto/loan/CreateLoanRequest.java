package co.com.crediya.api.dto.loan;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateLoanRequest(
        @NotNull  @DecimalMin(value = "0.00") BigDecimal amount,
        @NotNull  @Min(1) Integer termMonths,
        @NotBlank @Email  String email,
        @NotBlank String typeLoanId,   // state is usually set by system as PENDING
        String stateLoanId             // optional; could be null to default
) {}
