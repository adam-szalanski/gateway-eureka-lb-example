package com.example;

import io.swagger.v3.oas.models.media.Schema;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        System.setProperty(Schema.BIND_TYPE_AND_TYPES, Boolean.TRUE.toString());
        SpringApplication.run(GatewayApplication.class, args);
    }

}
