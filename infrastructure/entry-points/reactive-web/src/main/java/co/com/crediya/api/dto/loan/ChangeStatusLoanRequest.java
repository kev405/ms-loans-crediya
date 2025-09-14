package co.com.crediya.api.dto.loan;

public record ChangeStatusLoanRequest(String loanId, String newStateId, String reason) {
}
