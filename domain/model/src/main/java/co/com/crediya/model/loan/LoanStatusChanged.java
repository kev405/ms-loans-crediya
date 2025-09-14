package co.com.crediya.model.loan;

import co.com.crediya.model.customer.UserData;
import co.com.crediya.model.loan.Loan;

public record LoanStatusChanged(Loan loan, String stateName, String typeName, String reason, UserData userData) {}
