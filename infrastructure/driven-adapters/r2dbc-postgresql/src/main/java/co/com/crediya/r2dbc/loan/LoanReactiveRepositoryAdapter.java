package co.com.crediya.r2dbc.loan;

import co.com.crediya.model.loan.Loan;
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

    public LoanReactiveRepositoryAdapter(LoanReactiveRepository repository, ObjectMapper mapper, LoanEntityMapper entityMapper,
                                         DatabaseClient client) {
        super(repository, mapper, entityMapper::toDomain);
        this.entityMapper = entityMapper;
        this.client = client;
    }

    @Override
    public Mono<Loan> save(Loan loan) {


        LoanEntity entity = entityMapper.toEntity(loan);
        return repository.save(entity).map(entityMapper::toDomain);
    }

    @Override public Mono<Loan> findById(UUID id) { return repository.findById(id).map(entityMapper::toDomain); }
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
                .map(tuple -> {
                    List<LoanSummary> content = tuple.getT1();
                    long totalElements = tuple.getT2();
                    return new Pageable<>(content, totalElements, safePage, safeSize);
                });
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

//    @Override
//    public Mono<Pageable<LoanSummary>> findForManualReview(ManualReviewFilter f, int page, int size) {
//        int offset = Math.max(page, 0) * Math.max(size, 1);
//
//        Set<LoanStatus> statuses = (f.statuses() == null || f.statuses().isEmpty())
//                ? Set.of(LoanStatus.PENDING_REVIEW, LoanStatus.REJECTED, LoanStatus.MANUAL_REVIEW)
//                : f.statuses();
//        List<String> statusArray = statuses.stream().map(Enum::name).toList();
//
//        // ---------- LISTA ----------
//        var listSpec = client.sql("""
//            SELECT  l.id,
//                    l.amount,
//                    l.term_months,
//                    l.email                         AS applicant_email,
//                    NULL::varchar                   AS applicant_name,       -- sin users
//                    tl.name                         AS type_loan_name,
//                    (tl.annual_interest_percent / 12.0 / 100.0) :: numeric  AS interest_rate_monthly,
//                    ls.name                         AS status,
//                    NULL::numeric                   AS base_salary,          -- sin users
//                    COALESCE((
//                       SELECT SUM(
//                           ROUND(
//                               l2.amount
//                               * (lt2.annual_interest_percent / 12.0 / 100.0)
//                               * POWER(1 + (lt2.annual_interest_percent / 12.0 / 100.0), l2.term_months)
//                               / (POWER(1 + (lt2.annual_interest_percent / 12.0 / 100.0), l2.term_months) - 1),
//                           2)
//                       )
//                       FROM loan l2
//                       JOIN loan_type  lt2 ON lt2.id = l2.id_type_loan
//                       JOIN loan_state ls2 ON ls2.id = l2.id_state_loan AND ls2.name = 'APPROVED'
//                       WHERE l2.email = l.email
//                    ), 0) AS monthly_approved_debt
//            FROM loan l
//            JOIN loan_type  tl ON tl.id = l.id_type_loan
//            JOIN loan_state ls ON ls.id = l.id_state_loan
//            WHERE ls.name = ANY(:status_array)                              -- <-- corregido
//              AND (:search    IS NULL OR l.email ILIKE '%'||:search||'%')
//              AND (:typeId    IS NULL OR l.id_type_loan = :typeId)
//              AND (:minAmount IS NULL OR l.amount >= :minAmount)
//              AND (:maxAmount IS NULL OR l.amount <= :maxAmount)
//            ORDER BY l.created_at DESC
//            OFFSET :offset LIMIT :limit
//        """);
//
//        // binds (mantén String[] para ANY)
//        listSpec = listSpec.bind("status_array", statusArray.toArray(String[]::new));
//        listSpec = bindNullable(listSpec, "search", emptyToNull(f.search()), String.class);
//        listSpec = bindNullable(listSpec, "typeId",
//                (f.typeLoanId() == null || f.typeLoanId().isBlank()) ? null : UUID.fromString(f.typeLoanId()),
//                UUID.class);
//        listSpec = bindNullable(listSpec, "minAmount", f.minAmount(), BigDecimal.class);
//        listSpec = bindNullable(listSpec, "maxAmount", f.maxAmount(), BigDecimal.class);
//        listSpec = listSpec.bind("offset", offset).bind("limit", size);
//
//        // mapeo (nota el alias interest_rate_monthly calculado y status por nombre)
//        Flux<LoanSummary> rows = listSpec.map((row, meta) -> new LoanSummary(
//                row.get("id", String.class),
//                row.get("amount", BigDecimal.class),
//                row.get("term_months", Integer.class),
//                row.get("applicant_email", String.class),
//                row.get("applicant_name", String.class),
//                row.get("type_loan_name", String.class),
//                row.get("interest_rate_monthly", BigDecimal.class), // <-- viene del cálculo
//                // si LoanSummary espera enum:
//                // LoanStatus.valueOf(row.get("status", String.class)),
//                row.get("status", String.class),
//                row.get("base_salary", BigDecimal.class),
//                row.get("monthly_approved_debt", BigDecimal.class)
//        )).all();
//
//        // ---------- TOTAL ----------
//        var countSpec = client.sql("""
//            SELECT COUNT(1)
//            FROM loan l
//            JOIN loan_state ls ON ls.id = l.id_state_loan
//            WHERE ls.name = ANY(:status_array)
//              AND (:search    IS NULL OR l.email ILIKE '%'||:search||'%')
//              AND (:typeId    IS NULL OR l.id_type_loan = :typeId)
//              AND (:minAmount IS NULL OR l.amount >= :minAmount)
//              AND (:maxAmount IS NULL OR l.amount <= :maxAmount)
//        """);
//
//        countSpec = countSpec.bind("status_array", statusArray.toArray(String[]::new));
//        countSpec = bindNullable(countSpec, "search", emptyToNull(f.search()), String.class);
//        countSpec = bindNullable(countSpec, "typeId",
//                (f.typeLoanId() == null || f.typeLoanId().isBlank()) ? null : UUID.fromString(f.typeLoanId()),
//                UUID.class);
//        countSpec = bindNullable(countSpec, "minAmount", f.minAmount(), BigDecimal.class);
//        countSpec = bindNullable(countSpec, "maxAmount", f.maxAmount(), BigDecimal.class);
//
//        Mono<Long> total = countSpec.map((r, m) -> r.get(0, Long.class))
//                .one()
//                .defaultIfEmpty(0L);
//
//        return rows.collectList().zipWith(total)
//                .map(t -> new Pageable<>(t.getT1(), t.getT2(), page, size));
//
//    }
//
//    private static String emptyToNull(String s) {
//        return (s == null || s.isBlank()) ? null : s;
//    }
//
//    /** Helper para bindear nulos correctamente */
//    private static DatabaseClient.GenericExecuteSpec bindNullable(
//            DatabaseClient.GenericExecuteSpec spec, String name, Object value, Class<?> type) {
//        return (value == null) ? spec.bindNull(name, type) : spec.bind(name, value);
//    }

}
