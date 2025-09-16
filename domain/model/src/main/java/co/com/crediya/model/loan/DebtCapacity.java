package co.com.crediya.model.loan;

import co.com.crediya.model.customer.UserData;
import java.util.List;

public record DebtCapacity(Loan loan, List<LoanApproved> loanApprovedList, UserData userData) {}
