package co.com.crediya.usecase.loan;

import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.model.exceptions.DomainNotFoundException;
import co.com.crediya.model.exceptions.DomainValidationException;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.model.stateloan.gateways.StateLoanRepository;
import co.com.crediya.model.tx.gateway.TxRunner;
import co.com.crediya.model.typeloan.TypeLoan;
import co.com.crediya.model.typeloan.gateways.TypeLoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RequiredArgsConstructor
@Log
public class LoanUseCase {

    private final LoanRepository loanRepository;
    private final TypeLoanRepository typeLoanRepository;
    private final StateLoanRepository stateLoanRepository;
    private final CustomerGateway customerGateway;
    private final TxRunner txRunner;

    public static final String DEFAULT_PENDING_STATE_NAME = "PENDING_REVIEW";

    public Mono<Loan> create(Loan loan) {
         log.info("Creating loan. email= " + loan.email().value() +
                 ", typeLoanId= " + loan.typeLoanId() +
                 ", amount= " + loan.amount().value().toString());

        return txRunner.required(() -> customerGateway.existsByEmail(loan.email().value())
                .flatMap(exists -> {
                    if (!exists) return Mono.error(new DomainValidationException("CUSTOMER_NOT_FOUND", "No customer found with email: " + loan.email().value()));
                    return Mono.empty();
                })
                .then(typeLoanRepository.findById(
                                UUID.fromString(loan.typeLoanId()))
                        .switchIfEmpty(Mono.error(new DomainNotFoundException("TYPE_LOAN_NOT_FOUND"))))
                .flatMap(type -> validateAmountInRange(loan, type))
                .then(stateLoanRepository.findByName(DEFAULT_PENDING_STATE_NAME)
                        .switchIfEmpty(Mono.error(new DomainNotFoundException("STATE_PENDING_REVIEW_NOT_FOUND"))))
                .flatMap(pendingState -> {
                    var loanToSave = new Loan(
                            null,
                            loan.amount(),
                            loan.termMonths(),
                            loan.email(),
                            pendingState.id(),
                            loan.typeLoanId()
                    );
                    return loanRepository.save(loanToSave);
                })
                .doOnSuccess(saved -> log.info("Loan created id= " + saved.id())));
    }

    private Mono<Void> validateAmountInRange(Loan loan, TypeLoan type) {
        var amount = loan.amount().value();
        var min = type.minimumAmount().value();
        var max = type.maximumAmount().value();
        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            return Mono.error(new DomainValidationException("AMOUNT_OUT_OF_RANGE", "Amount must be between " + min + " and " + max));
        }
        return Mono.empty();
    }

    public Flux<Loan> getAllLoans() {
        log.info("Getting all loans");
        return txRunner.readOnlyMany(loanRepository::findAll);
    }
}