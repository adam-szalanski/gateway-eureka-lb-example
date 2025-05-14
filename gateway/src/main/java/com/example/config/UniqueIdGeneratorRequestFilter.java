package com.example.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class UniqueIdGeneratorRequestFilter implements GlobalFilter, Ordered {

    private static final String UNIQUE_ID_HEADER = "X-Unique-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Mono<Void> requestHandlingCompletionIndicator;

        if (!request.getHeaders().containsKey(UNIQUE_ID_HEADER)) {
            String uniqueId = UUID.randomUUID().toString();
            ServerHttpRequest modifiedRequest = request
                    .mutate()
                    .header(UNIQUE_ID_HEADER, uniqueId)
                    .build();
            requestHandlingCompletionIndicator = chain.filter(exchange.mutate().request(modifiedRequest).build());
        } else {
            requestHandlingCompletionIndicator = chain.filter(exchange);
        }

        return requestHandlingCompletionIndicator;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
