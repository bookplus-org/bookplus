package com.bookplus.order.adapter.in.web.sse;

/** Evento interno (in-process) que notifica que un pedido cambió, para empujarlo por SSE. */
public record OrderChangedAppEvent(String userId, String orderId, String status) {}
