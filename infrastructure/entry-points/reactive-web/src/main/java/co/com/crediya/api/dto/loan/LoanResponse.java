package co.com.crediya.api.dto.loan;

import java.math.BigDecimal;

public record LoanResponse(
        String id,
        BigDecimal amount,
        Integer termMonths,
        String email,
        String stateLoanId,
        String typeLoanId
) {}
