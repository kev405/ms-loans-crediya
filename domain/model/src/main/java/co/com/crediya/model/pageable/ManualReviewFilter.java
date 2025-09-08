package co.com.crediya.model.pageable;

import java.math.BigDecimal;
import java.util.Set;

public record ManualReviewFilter(
        String search,
        Set<LoanStatus> statuses,
        String typeLoanId,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {}
