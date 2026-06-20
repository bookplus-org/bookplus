package com.bookplus.catalog.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Caché HTTP condicional (ETag) para las lecturas del catálogo.
 *
 * El {@link ShallowEtagHeaderFilter} calcula un hash (ETag) del cuerpo de la respuesta y lo
 * devuelve en la cabecera {@code ETag}. En la siguiente petición el cliente envía
 * {@code If-None-Match: <etag>}; si el contenido no cambió, el servidor responde
 * {@code 304 Not Modified} SIN cuerpo. El cliente reutiliza su copia: se ahorra ancho de
 * banda y trabajo de serialización, sin tocar la lógica de los controllers.
 */
@Configuration
public class EtagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/v1/books/*", "/api/v1/categories/*");
        registration.setName("etagFilter");
        return registration;
    }
}
