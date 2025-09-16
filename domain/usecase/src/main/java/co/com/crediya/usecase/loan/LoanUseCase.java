package co.com.crediya.usecase.loan;

import co.com.crediya.model.customer.UserData;
import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.model.exceptions.DomainNotFoundException;
import co.com.crediya.model.exceptions.DomainValidationException;
import co.com.crediya.model.loan.*;
import co.com.crediya.model.loan.gateways.DebtCapacitySQS;
import co.com.crediya.model.loan.gateways.LoanRepository;
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
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Log
public class LoanUseCase {

    private final LoanRepository loanRepository;

    private final TypeLoanRepository typeLoanRepository;

    private final StateLoanRepository stateLoanRepository;

    private final CustomerGateway customerGateway;

    private final DebtCapacitySQS debtCapacitySQS;

    private final TxRunner txRunner;

    public static final String DEFAULT_PENDING_STATE_NAME = "PENDING_REVIEW";

    public static final String DEFAULT_APPROVED_STATE_NAME = "APPROVED";

    public Mono<Loan> create(Loan loan) {
        log.info("Creating loan. email= " + loan.email().value() +
                ", typeLoanId= " + loan.typeLoanId() +
                ", amount= " + loan.amount().value().toString());

        Mono<TypeLoan> typeLoanMono =
                typeLoanRepository.findById(UUID.fromString(loan.typeLoanId()))
                        .switchIfEmpty(Mono.error(new DomainNotFoundException(
                                "TYPE_LOAN_NOT_FOUND")));

        Mono<StateLoan> pendingStateMono =
                stateLoanRepository.findByName(DEFAULT_PENDING_STATE_NAME)
                        .switchIfEmpty(Mono.error(new DomainNotFoundException(
                                "STATE_PENDING_REVIEW_NOT_FOUND")));


        return txRunner.required(
                () -> Mono.zip(typeLoanMono, pendingStateMono)
                        .flatMap(tuple -> {
                            TypeLoan type = tuple.getT1();
                            StateLoan pendingState = tuple.getT2();

                            return validateAmountInRange(loan,
                                    type)        // <- encadenado
                                    .then(Mono.defer(() -> {
                                        if (type.automaticValidation()) {
                                            Mono<UserData> userDataMono =
                                                    customerGateway.findByEmail(
                                                            loan.email()
                                                                    .value());
                                            Mono<List<LoanApproved>>
                                                    loanApprovedListMono =
                                                    getListLoanApproved(
                                                            loan.email()
                                                                    .value());

                                            return Mono.zip(userDataMono,
                                                            loanApprovedListMono)
                                                    .flatMap(t -> {
                                                        UserData userData =
                                                                t.getT1();
                                                        List<LoanApproved>
                                                                loanApprovedList =
                                                                t.getT2();

                                                        var loanToSave =
                                                                new Loan(
                                                                        null,
                                                                        loan.amount(),
                                                                        loan.termMonths(),
                                                                        loan.email(),
                                                                        pendingState.id(),
                                                                        loan.typeLoanId()
                                                                );

                                                        return loanRepository.save(
                                                                        loanToSave)
                                                                .flatMap(
                                                                        loanSaved -> {
                                                                            var
                                                                                    debtCapacity =
                                                                                    new DebtCapacity(
                                                                                            loanSaved,
                                                                                            loanApprovedList,
                                                                                            userData);
                                                                            return debtCapacitySQS.sendMessage(
                                                                                            debtCapacity)
                                                                                    .thenReturn(
                                                                                            loanSaved);
                                                                        });
                                                    });
                                        }
                                        var loanToSave = new Loan(
                                                null, loan.amount(),
                                                loan.termMonths(),
                                                loan.email(), pendingState.id(),
                                                loan.typeLoanId()
                                        );
                                        return loanRepository.save(loanToSave);
                                    }));
                        })
                        .doOnSuccess(saved -> log.info(
                                "Loan created id= " + saved.id()))
        );
    }

    public Mono<LoanStatusChanged> changeLoanStatus(
            ChangeLoanStatus changeLoanStatus) {
        log.info("Changing loan status. loanId= " + changeLoanStatus.loanId() +
                ", newStateId= " + changeLoanStatus.newStateId());
        return txRunner.required(() -> loanRepository.findById(
                        UUID.fromString(changeLoanStatus.loanId()))
                .switchIfEmpty(Mono.error(
                        new DomainNotFoundException("LOAN_NOT_FOUND")))
                .flatMap(loan -> {
                    Mono<StateLoan> newStateMono;
                    try {
                        newStateMono = stateLoanRepository.findById(UUID.fromString(changeLoanStatus.newStateId()));
                    } catch (IllegalArgumentException e) {
                        newStateMono = stateLoanRepository.findByName(changeLoanStatus.newStateId());
                    }
                    return newStateMono.switchIfEmpty(Mono.error(new DomainNotFoundException(
                                            "STATE_LOAN_NOT_FOUND")))
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
                                    });
                        }
                ).flatMap(saved ->
                        Mono.zip(
                                stateLoanRepository.findById(
                                                UUID.fromString(saved.stateLoanId()))
                                        .map(StateLoan::name),
                                typeLoanRepository.findById(
                                                UUID.fromString(saved.typeLoanId()))
                                        .map(TypeLoan::name),
                                customerGateway.findByEmail(
                                        saved.email().value())
                        ).map(t -> new LoanStatusChanged(saved, t.getT1(),
                                t.getT2(),
                                changeLoanStatus.reason(), t.getT3()))
                ));
    }

    private Mono<Void> validateAmountInRange(Loan loan, TypeLoan type) {
        var amount = loan.amount().value();
        var min = type.minimumAmount().value();
        var max = type.maximumAmount().value();
        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            return Mono.error(
                    new DomainValidationException("AMOUNT_OUT_OF_RANGE",
                            "Amount must be between " + min + " and " + max));
        }
        return Mono.empty();
    }

    public Flux<Loan> getAllLoans() {
        log.info("Getting all loans");
        return txRunner.readOnlyMany(loanRepository::findAll);
    }

    public Mono<Pageable<LoanSummary>> execute(ManualReviewFilter filter,
                                               int page, int size) {
        return txRunner.readOnly(
                () -> loanRepository.findForManualReview(filter, page, size));
    }

    private Mono<List<LoanApproved>> getListLoanApproved(String email) {
        log.info("Getting mensual debt for loans with email= " + email);
        return stateLoanRepository.findByName(DEFAULT_APPROVED_STATE_NAME)
                .flatMap(stateLoan -> loanRepository.findByEmailAndStatusId(email,
                                UUID.fromString(stateLoan.id())).collectList());
    }

    /// Helper method to Debt Capacity Opcion 1

//    private Mono<BigDecimal> getTotalMensualDebt(String email) {
//        log.info("Getting mensual debt for loans with email= " + email);
//        return stateLoanRepository.findByName(DEFAULT_APPROVED_STATE_NAME)
//                .flatMap(stateLoan -> loanRepository.findByEmailAndStatusId(email,
//                                UUID.fromString(stateLoan.id()))
//                        .flatMap(loan -> {
//                            Mono<TypeLoan> typeLoan = typeLoanRepository.findById(
//                                    UUID.fromString(loan.typeLoanId()));
//                            return Mono.zip(Mono.just(loan), typeLoan);
//                        })
//                        .flatMap(tuple -> {
//                            Loan loan = tuple.getT1();
//                            TypeLoan typeLoan = tuple.getT2();
//                            log.info(loan.toString());
//                            BigDecimal mensualDebt = monthlyFee(
//                                    loan.amount().value(),
//                                    loan.termMonths().value(),
//                                    typeLoan.annualInterestRate().annualPercent());
//                            return Mono.just(mensualDebt);
//                        })
//                        .reduce(BigDecimal.ZERO, BigDecimal::add)
//                ).defaultIfEmpty(BigDecimal.ZERO);
//    }
//
//    private BigDecimal monthlyFee(BigDecimal amount, int termMonths,
//                                  BigDecimal annualInterestRate) {
//        log.info("Calculating monthly fee. amount= " + amount.toString() +
//                ", termMonths= " + termMonths +
//                ", annualInterestRate= " + annualInterestRate.toString());
//
//        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
//            if (termMonths == 0) return amount;
//            return amount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
//        }
//
//        BigDecimal monthlyRate = annualInterestRate
//                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
//                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
//
//        BigDecimal numerator = amount.multiply(monthlyRate);
//
//        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
//        BigDecimal termFactor = onePlusRate.pow(-termMonths, MathContext.DECIMAL128);
//        BigDecimal denominator = BigDecimal.ONE.subtract(termFactor);
//
//        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
//            return BigDecimal.ZERO;
//        }
//
//        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
//    }
}