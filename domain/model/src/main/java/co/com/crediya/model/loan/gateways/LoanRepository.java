package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.pageable.LoanSummary;
import co.com.crediya.model.pageable.ManualReviewFilter;
import co.com.crediya.model.pageable.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface LoanRepository {
    Mono<Loan> save(Loan loan);
    Mono<Loan> findById(UUID id);
    Flux<Loan> findAll();
    Mono<Pageable<LoanSummary>> findForManualReview(ManualReviewFilter filter, int page, int size);
}
