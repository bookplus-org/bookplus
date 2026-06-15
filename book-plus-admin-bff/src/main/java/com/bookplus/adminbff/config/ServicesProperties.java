package com.bookplus.adminbff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URLs for all downstream microservices.
 * The BFF calls these via WebClient — no service discovery needed (Docker DNS).
 */
@ConfigurationProperties(prefix = "services")
@Getter @Setter
public class ServicesProperties {
    private String catalogUrl      = "http://localhost:8082";
    private String inventoryUrl    = "http://localhost:8083";
    private String orderUrl        = "http://localhost:8085";
    private String paymentUrl      = "http://localhost:8086";
    private String notificationUrl = "http://localhost:8087";
    private String reportUrl       = "http://localhost:8088";
    private String authUrl         = "http://localhost:8081";
}
