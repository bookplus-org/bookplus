package com.bookplus.catalog.domain.port.in;

import java.util.List;

/**
 * Resultado paginado genérico — sin dependencias de Spring Page.
 */
public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedResult<T> of(List<T> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResult<>(content, page, size, total, totalPages,
                page == 0, page >= totalPages - 1);
    }
}
