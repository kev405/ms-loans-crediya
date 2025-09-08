package co.com.crediya.api.dto.pageable;

public record ManualReviewQuery(
        String search,
        String status,
        String typeLoanId,
        Integer page,
        Integer size,
        String minAmount,
        String maxAmount
) {}
