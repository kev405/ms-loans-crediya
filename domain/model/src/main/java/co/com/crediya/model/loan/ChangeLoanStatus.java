package co.com.crediya.model.loan;

public record ChangeLoanStatus (String loanId, String newStateId, String reason) {
}
