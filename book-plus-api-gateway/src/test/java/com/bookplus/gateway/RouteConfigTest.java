package com.bookplus.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "gateway.jwt.public-key-base64=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwa8VY66yNivLSD+znTVO+FhRPlY/qMJe04SMPL7qSmTW2bOddlY5zLHpYj1poDs14H3/1ilLsF6s5WaHdJBDEr9zbMLhf1RPchpreQXAW/5++9wQBAo54vxWG6qYP/uo/BPN1EQC2lMZAhRZPwUMRMJxTqxRcRuDtd+JIo3GFpFxfo4tbgW1IlEdhfNob+Lg9Pzky8prUmPnLyW+gK6aTDTlyGDi3Goe09xE4lEF/daFB4rh91FI5gB48lmA5hJk/Xl5AVwysthr3NamdlSAcfhHjCA4jFld8JcKwpQg2Jki44L4/tzUkHyBOqqnbn7o34EPPy/Umdonm7PXsiy8IQIDAQAB",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@DisplayName("Gateway route configuration")
class RouteConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("All 7 service routes are registered")
    void allRoutesAreRegistered() {
        long count = routeLocator.getRoutes().count().block();
        assertThat(count).isGreaterThanOrEqualTo(7);
    }

    @Test
    @DisplayName("Route IDs cover all services")
    void routeIdsContainAllServices() {
        var ids = routeLocator.getRoutes()
                .map(r -> r.getId())
                .collectList()
                .block();

        assertThat(ids).contains(
                "auth-service",
                "catalog-service-books",
                "inventory-service",
                "cart-service",
                "order-service",
                "payment-service",
                "notification-service"
        );
    }
}
