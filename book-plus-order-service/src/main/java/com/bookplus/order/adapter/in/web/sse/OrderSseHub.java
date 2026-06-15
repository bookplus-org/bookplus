package com.bookplus.order.adapter.in.web.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registro de conexiones SSE por usuario. Cuando un pedido cambia (evento interno
 * {@link OrderChangedAppEvent}), empuja un ping a las conexiones de ese usuario para
 * que el frontend recargue al instante, sin esperar al polling.
 */
@Component
@Slf4j
public class OrderSseHub {

    /** 30 minutos; el navegador reconecta solo si se corta. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    /** Conexiones de personal (admin/repartidor): reciben TODOS los cambios. */
    private final List<SseEmitter> staffEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(String userId, boolean staff) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        if (staff) staffEmitters.add(emitter);

        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {
            remove(userId, emitter);
        }
        return emitter;
    }

    @EventListener
    public void onOrderChanged(OrderChangedAppEvent event) {
        Map<String, String> payload = Map.of(
                "orderId", event.orderId(), "status", event.status());

        // 1) Al dueño del pedido.
        List<SseEmitter> owners = emittersByUser.get(event.userId());
        if (owners != null) owners.forEach(em -> send(em, payload));

        // 2) A todo el personal conectado (admin/repartidor), evitando duplicar
        //    si el dueño también es personal.
        for (SseEmitter em : staffEmitters) {
            if (owners == null || !owners.contains(em)) send(em, payload);
        }
    }

    private void send(SseEmitter emitter, Map<String, String> payload) {
        try {
            emitter.send(SseEmitter.event().name("order-update").data(payload));
        } catch (Exception ex) {
            emitter.complete();
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        staffEmitters.remove(emitter);
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) emittersByUser.remove(userId);
        }
    }
}
