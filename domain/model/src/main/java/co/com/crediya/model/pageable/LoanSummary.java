package co.com.crediya.model.pageable;

import java.math.BigDecimal;

public record LoanSummary(
        String id,
        BigDecimal amount,
        Integer termMonths,
        String applicantEmail,
        String applicantName,        // si solo guardas email, puedes dejarlo null
        String typeLoanName,
        BigDecimal interestRateMonthly,  // o anual, como lo manejes
        String status,
        BigDecimal baseSalary,
        BigDecimal monthlyApprovedDebt // suma de cuotas de préstamos aprobados del solicitante
) {}
