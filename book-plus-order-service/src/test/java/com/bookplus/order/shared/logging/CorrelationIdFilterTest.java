package com.bookplus.order.shared.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el filtro de correlation-ID: genera o reutiliza el id, lo pone en el MDC durante
 * la petición, lo devuelve en la respuesta y lo limpia al terminar.
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void genera_id_si_no_viene_y_lo_limpia_al_terminar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> mdcDuringRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(mdcDuringRequest.get()).isNotBlank();                       // presente durante la petición
        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo(mdcDuringRequest.get());                            // devuelto en la respuesta
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();             // limpiado al terminar
    }

    @Test
    void reutiliza_el_id_entrante() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> seen.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(seen.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
    }
}
