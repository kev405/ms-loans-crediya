package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.Loan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface LoanRepository {
    Mono<Loan> save(Loan loan);
    Mono<Loan> findById(UUID id);
    Flux<Loan> findAll();
}
