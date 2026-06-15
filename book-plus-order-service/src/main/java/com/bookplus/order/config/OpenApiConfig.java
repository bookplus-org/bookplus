package com.bookplus.order.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "BookPlus — Order Service",
        version     = "v1",
        description = "Order lifecycle management. Consumes cart.checked-out; emits order.created, order.cancelled, order.status.changed.",
        contact     = @Contact(name = "BookPlus Engineering", email = "engineering@bookplus.com")
    ),
    servers = @Server(url = "http://localhost:8080", description = "API Gateway")
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {}
