package co.com.crediya.usecase.loan;

import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.model.exceptions.DomainNotFoundException;
import co.com.crediya.model.exceptions.DomainValidationException;
import co.com.crediya.model.loan.ChangeLoanStatus;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanStatusChanged;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.model.loan.gateways.Notification;
import co.com.crediya.model.pageable.LoanSummary;
import co.com.crediya.model.pageable.ManualReviewFilter;
import co.com.crediya.model.pageable.Pageable;
import co.com.crediya.model.stateloan.StateLoan;
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
    private final Notification notification;
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
//                .flatMap(exists -> {
//                    if (!exists) return Mono.error(new DomainValidationException("CUSTOMER_NOT_FOUND", "No customer found with email: " + loan.email().value()));
//                    return Mono.empty();
//                })
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

    public Mono<LoanStatusChanged> changeLoanStatus(ChangeLoanStatus changeLoanStatus) {
        log.info("Changing loan status. loanId= " + changeLoanStatus.loanId() +
                ", newStateId= " + changeLoanStatus.newStateId());
        return txRunner.required(() -> loanRepository.findById(UUID.fromString(changeLoanStatus.loanId()))
                .switchIfEmpty(Mono.error(new DomainNotFoundException("LOAN_NOT_FOUND")))
                .flatMap(loan -> stateLoanRepository.findById(UUID.fromString(changeLoanStatus.newStateId()))
                        .switchIfEmpty(Mono.error(new DomainNotFoundException("STATE_LOAN_NOT_FOUND")))
                        .flatMap(newState -> {
                            var updatedLoan = new Loan(
                                    loan.id(),
                                    loan.amount(),
                                    loan.termMonths(),
                                    loan.email(),
                                    newState.id(),
                                    loan.typeLoanId()
                            );
                            return loanRepository.save(updatedLoan);
                        })
                ).flatMap(saved ->
                        Mono.zip(
                                stateLoanRepository.findById(UUID.fromString(saved.stateLoanId())).map(StateLoan::name),
                                typeLoanRepository.findById(UUID.fromString(saved.typeLoanId())).map(TypeLoan::name),
                                customerGateway.findByEmail(saved.email().value())
                        ).map(t -> new LoanStatusChanged(saved, t.getT1(), t.getT2(),
                                changeLoanStatus.reason(), t.getT3()))
                ));
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

//    public Mono<Pageable<LoanSummary>> execute(ManualReviewFilter filter, int page, int size) {
//        return txRunner.readOnly(() -> loanRepository.findForManualReview(filter, page, size));
//    }

    public Mono<Pageable<LoanSummary>> execute(ManualReviewFilter filter, int page, int size) {
        return txRunner.readOnly(() -> loanRepository.findForManualReview(filter, page, size));
    }
}