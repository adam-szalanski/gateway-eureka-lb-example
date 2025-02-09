package com.example.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UniqueIdGeneratorGatewayFilterFactory extends AbstractGatewayFilterFactory<UniqueIdGeneratorGatewayFilterFactory.Config> {

    private static final String UNIQUE_ID_HEADER = "X-Unique-Id";

    public UniqueIdGeneratorGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(UNIQUE_ID_HEADER)) {
                String uniqueId = UUID.randomUUID().toString();
                ServerHttpRequest modifiedRequest = request
                        .mutate()
                        .header(UNIQUE_ID_HEADER, uniqueId)
                        .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }

            return chain.filter(exchange);
        });
    }

    public static class Config {
    }
}
