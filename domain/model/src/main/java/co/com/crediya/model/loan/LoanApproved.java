package co.com.crediya.model.loan;

import java.math.BigDecimal;

public record LoanApproved(String id, BigDecimal amount, Integer termMonths, String typeLoanName, BigDecimal annualInterestRate) {
}
