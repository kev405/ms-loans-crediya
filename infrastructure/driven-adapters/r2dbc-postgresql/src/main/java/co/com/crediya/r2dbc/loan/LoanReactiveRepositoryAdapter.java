package co.com.crediya.r2dbc.loan;

import co.com.crediya.model.customer.gateways.CustomerGateway;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanApproved;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.model.pageable.LoanStatus;
import co.com.crediya.model.pageable.LoanSummary;
import co.com.crediya.model.pageable.ManualReviewFilter;
import co.com.crediya.model.pageable.Pageable;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import co.com.crediya.r2dbc.loan.entity.LoanEntity;
import co.com.crediya.r2dbc.loan.mapper.LoanEntityMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import io.r2dbc.spi.Row;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

@Repository
public class LoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Loan,
        LoanEntity,
        UUID,
        LoanReactiveRepository
> implements LoanRepository {

    private final LoanEntityMapper entityMapper;

    private final DatabaseClient client;

    private final CustomerGateway customerGateway;

    public LoanReactiveRepositoryAdapter(LoanReactiveRepository repository, ObjectMapper mapper, LoanEntityMapper entityMapper,
                                         DatabaseClient client,
                                         CustomerGateway customerGateway) {
        super(repository, mapper, entityMapper::toDomain);
        this.entityMapper = entityMapper;
        this.client = client;
        this.customerGateway = customerGateway;
    }

    @Override
    public Mono<Loan> save(Loan loan) {


        LoanEntity entity = entityMapper.toEntity(loan);
        return repository.save(entity).map(entityMapper::toDomain);
    }

    @Override public Mono<Loan> findById(UUID id) { return repository.findById(id).map(entityMapper::toDomain); }

    @Override
    public Flux<LoanApproved> findByEmailAndStatusId(String email, UUID statusId) {
        return repository.findByEmailAndStateLoanId(email, statusId);
    }

    @Override public Flux<Loan> findAll() { return repository.findAll().map(entityMapper::toDomain); }

    @Override
    public Mono<Pageable<LoanSummary>> findForManualReview(ManualReviewFilter f, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        long offset = (long) safePage * safeSize;

        Set<LoanStatus> statuses = (f.statuses() == null || f.statuses().isEmpty())
                ? Set.of(LoanStatus.PENDING_REVIEW, LoanStatus.REJECTED, LoanStatus.MANUAL_REVIEW)
                : f.statuses();
        String[] statusArray = statuses.stream().map(Enum::name).toArray(String[]::new);

        String search = emptyToNull(f.search());
        UUID typeId = (f.typeLoanId() == null || f.typeLoanId().isBlank())
                ? null
                : UUID.fromString(f.typeLoanId());
        BigDecimal minAmount = f.minAmount();
        BigDecimal maxAmount = f.maxAmount();

        Flux<LoanSummary> rows = repository.findForManualReview(
                        statusArray, search, typeId, minAmount, maxAmount, offset, safeSize
                );

        Mono<Long> total = repository.countForManualReview(statusArray, search, typeId, minAmount, maxAmount);

        return rows.collectList()
                .zipWith(total.defaultIfEmpty(0L))
                .flatMap(tuple -> {
                    List<LoanSummary> content = tuple.getT1();
                    long totalElements = tuple.getT2();

                    Mono<List<LoanSummary>> contentWithUserData = Flux.fromIterable(content)
                            .flatMap(loanSummary -> customerGateway.findByEmail(loanSummary.applicantEmail())
                                    .map(userData -> {
                                        return new LoanSummary(
                                                loanSummary.id(),
                                                loanSummary.amount(),
                                                loanSummary.termMonths(),
                                                loanSummary.applicantEmail(),
                                                userData.name() + " " + userData.lastName(),
                                                loanSummary.typeLoanName(),
                                                loanSummary.interestRateMonthly(),
                                                loanSummary.status(),
                                                userData.salary(),
                                                loanSummary.monthlyApprovedDebt()
                                        );
                                    })
                                    .defaultIfEmpty(new LoanSummary(
                                            loanSummary.id(),
                                            loanSummary.amount(),
                                            loanSummary.termMonths(),
                                            loanSummary.applicantEmail(),
                                            null, // applicant_name si no se encuentra el usuario
                                            loanSummary.typeLoanName(),
                                            loanSummary.interestRateMonthly(),
                                            loanSummary.status(),
                                            null, // base_salary si no se encuentra el usuario
                                            loanSummary.monthlyApprovedDebt()
                                    ))
                            )
                            .collectList();

                    return contentWithUserData.map(contentWithUsers ->
                            new Pageable<>(contentWithUsers, totalElements, safePage, safeSize));
                });
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

}
