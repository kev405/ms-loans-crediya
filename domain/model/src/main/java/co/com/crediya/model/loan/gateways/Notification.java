package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanStatusChanged;
import reactor.core.publisher.Mono;

public interface Notification {

    Mono<Void> sendMessage(LoanStatusChanged loanStatusChanged);

}
