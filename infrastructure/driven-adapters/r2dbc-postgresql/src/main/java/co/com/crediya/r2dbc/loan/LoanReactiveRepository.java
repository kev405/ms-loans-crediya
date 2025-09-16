package co.com.crediya.r2dbc.loan;

import co.com.crediya.model.loan.LoanApproved;
import co.com.crediya.model.pageable.LoanSummary;
import co.com.crediya.r2dbc.loan.entity.LoanEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// TODO: This file is just an example, you should delete or modify it
public interface LoanReactiveRepository extends ReactiveCrudRepository<LoanEntity, UUID>, ReactiveQueryByExampleExecutor<LoanEntity> {

    @Query("""
        SELECT  l.id,
                l.amount,
                l.term_months                               AS term_months,
                l.email                                      AS applicant_email,
                NULL::varchar                                AS applicant_name,       -- sin users
                tl.name                                      AS type_loan_name,
                (tl.annual_interest_percent / 12.0 / 100.0)::numeric  AS interest_rate_monthly,
                ls.name                                      AS status,
                NULL::numeric                                AS base_salary,          -- sin users
                COALESCE((
                    SELECT SUM(ROUND(
                        l2.amount
                        * (lt2.annual_interest_percent / 12.0 / 100.0)
                        * POWER(1 + (lt2.annual_interest_percent / 12.0 / 100.0), l2.term_months)
                        / (POWER(1 + (lt2.annual_interest_percent / 12.0 / 100.0), l2.term_months) - 1),
                        2
                    ))
                    FROM loan l2
                    JOIN loan_type  lt2 ON lt2.id = l2.id_type_loan
                    JOIN loan_state ls2 ON ls2.id = l2.id_state_loan AND ls2.name = 'APPROVED'
                    WHERE l2.email = l.email
                ), 0) AS monthly_approved_debt
        FROM loan l
        JOIN loan_type  tl ON tl.id = l.id_type_loan
        JOIN loan_state ls ON ls.id = l.id_state_loan
        WHERE ls.name = ANY(:statuses)
          AND (:search    IS NULL OR l.email ILIKE '%'||:search||'%')
          AND (:typeId    IS NULL OR l.id_type_loan = :typeId)
          AND (:minAmount IS NULL OR l.amount >= :minAmount)
          AND (:maxAmount IS NULL OR l.amount <= :maxAmount)
        ORDER BY l.created_at DESC
        OFFSET :offset LIMIT :limit
    """)
    Flux<LoanSummary> findForManualReview(
            @Param("statuses") String[] statuses,
            @Param("search") String search,
            @Param("typeId") UUID typeLoanId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Query("""
        SELECT COUNT(1)
        FROM loan l
        JOIN loan_type  tl ON tl.id = l.id_type_loan
        JOIN loan_state ls ON ls.id = l.id_state_loan
        WHERE ls.name = ANY(:statuses)
          AND (:search    IS NULL OR l.email ILIKE '%'||:search||'%')
          AND (:typeId    IS NULL OR l.id_type_loan = :typeId)
          AND (:minAmount IS NULL OR l.amount >= :minAmount)
          AND (:maxAmount IS NULL OR l.amount <= :maxAmount)
    """)
    Mono<Long> countForManualReview(
            @Param("statuses") String[] statuses,
            @Param("search") String search,
            @Param("typeId") UUID typeLoanId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount
    );

    @Query("""
        SELECT l.id,
               l.amount,
               l.term_months                               AS term_months,
               l.email                                      AS applicant_email,
               tl.name                                      AS type_loan_name,
               tl.annual_interest_percent::numeric  AS annual_interest_rate
        FROM loan l
        JOIN loan_type  tl ON tl.id = l.id_type_loan
        WHERE l.email = :email
          AND l.id_state_loan = :stateLoanId
        ORDER BY l.created_at DESC
    """)
    Flux<LoanApproved> findByEmailAndStateLoanId(String email, UUID statusId);
}
