package com.bookplus.catalog.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el comportamiento de ETag / 304 Not Modified que registra EtagConfig.
 */
class EtagFilterTest {

    private final ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();

    @Test
    void genera_etag_y_responde_304_si_el_contenido_no_cambio() throws Exception {
        byte[] body = "{\"id\":\"1\",\"title\":\"Clean Code\"}".getBytes();

        // 1) Primera petición: 200 + ETag calculado del cuerpo.
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/v1/books/1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(req1, res1, (rq, rs) -> rs.getOutputStream().write(body));

        String etag = res1.getHeader("ETag");
        assertThat(res1.getStatus()).isEqualTo(200);
        assertThat(etag).isNotBlank();

        // 2) Segunda petición con If-None-Match: 304 sin cuerpo.
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/v1/books/1");
        req2.addHeader("If-None-Match", etag);
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req2, res2, (rq, rs) -> rs.getOutputStream().write(body));

        assertThat(res2.getStatus()).isEqualTo(304);
        assertThat(res2.getContentAsByteArray()).isEmpty();
    }
}
