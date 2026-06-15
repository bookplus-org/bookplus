package com.bookplus.catalog.adapter.in.web.dto;

import com.bookplus.catalog.domain.port.in.PagedResult;

import java.util.List;
import java.util.function.Function;

/** DTO genérico para respuestas paginadas. */
public record PagedResponse<T>(
        List<T>  content,
        int      page,
        int      size,
        long     totalElements,
        int      totalPages,
        boolean  first,
        boolean  last
) {
    public static <D, S> PagedResponse<D> from(PagedResult<S> result, Function<S, D> mapper) {
        return new PagedResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.first(),
                result.last()
        );
    }
}
