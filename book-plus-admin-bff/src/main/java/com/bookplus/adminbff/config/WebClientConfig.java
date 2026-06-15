package com.bookplus.adminbff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides a WebClient builder with the admin JWT forwarded downstream.
 * Each controller injects this builder and sets the service base URL.
 */
@Configuration
public class WebClientConfig {

    /**
     * Base WebClient builder — individual controllers set baseUrl per request.
     * The Authorization header is forwarded from the incoming request automatically
     * via the TokenRelayGatewayFilter-equivalent logic in the BFF controllers.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)); // 2 MB
    }
}
