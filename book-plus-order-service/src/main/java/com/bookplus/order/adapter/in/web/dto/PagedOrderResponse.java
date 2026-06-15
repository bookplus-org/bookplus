package com.bookplus.order.adapter.in.web.dto;

import java.util.List;

public record PagedOrderResponse(
        List<OrderResponse> content,
        int    page,
        int    size,
        long   totalElements,
        int    totalPages
) {
    public static PagedOrderResponse of(List<OrderResponse> content,
                                        int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PagedOrderResponse(content, page, size, totalElements, totalPages);
    }
}
