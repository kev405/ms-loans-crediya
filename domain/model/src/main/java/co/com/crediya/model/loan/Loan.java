package co.com.crediya.model.loan;

import co.com.crediya.model.value.Email;
import co.com.crediya.model.value.Money;
import co.com.crediya.model.value.TermMonths;
import java.util.Objects;

public record Loan(
        String id,
        Money amount,
        TermMonths termMonths,
        Email email,
        String stateLoanId,  // FK to StateLoan
        String typeLoanId    // FK to TypeLoan
) {
    public Loan {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(termMonths, "termMonths is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(typeLoanId, "typeLoanId is required");
    }
}
