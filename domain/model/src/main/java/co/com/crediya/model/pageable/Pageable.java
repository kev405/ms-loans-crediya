package co.com.crediya.model.pageable;

import java.util.List;

public record Pageable<T>(List<T> content, long totalElements, int page, int size) {
    public int totalPages() { return (int) Math.ceil((double) totalElements / (double) size); }
}
