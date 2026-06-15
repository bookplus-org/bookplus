package com.bookplus.adminbff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminBffApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminBffApplication.class, args);
    }
}
