package com.bookplus.cart.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "BookPlus — Cart Service",
        version     = "v1",
        description = "Shopping cart management. Backed by Redis; emits domain events consumed by order-service.",
        contact     = @Contact(name = "BookPlus Engineering", email = "engineering@bookplus.com")
    ),
    servers = @Server(url = "http://localhost:8080", description = "API Gateway")
)
@SecurityScheme(
    name            = "bearerAuth",
    type            = SecuritySchemeType.HTTP,
    scheme          = "bearer",
    bearerFormat    = "JWT",
    description     = "JWT issued by auth-service (RS256)"
)
public class OpenApiConfig {}
