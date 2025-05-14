package com.example.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class OpenApiValidationGatewayFilterFactory extends
                                                   AbstractGatewayFilterFactory<OpenApiValidationGatewayFilterFactory.Config> {

    private final OpenApiInteractionValidator validator = OpenApiInteractionValidator
            .createFor(new ClassPathResource("openapi/customer_service-openapi.yaml").getPath())
            .build();

    public OpenApiValidationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath()
                    .value();
            String method = request.getMethod()
                    .name();
            return request.getBody()
                    .map(this::extractBody)
                    .reduce(String::concat)
                    .flatMap(body -> validateRequest(exchange, chain, body, request, method, path));
        };
    }

    private Mono<Void> validateRequest(ServerWebExchange exchange, GatewayFilterChain chain, String body,
                                       ServerHttpRequest request, String method, String path) {
        Map<String, String> headers = request.getHeaders()
                .toSingleValueMap();
        log.info("Preparing to validate request: method: {}, path: {}, headers: {}, body:{}", method,
                 path, headers,
                 body);
        SimpleRequest.Builder builder = getValidationRequestBuilder(method, path, headers, body);

        ValidationReport validationReport = validator.validateRequest(
                builder.build());
        log.info("Validation report: {}", validationReport);

        return verifyValidationReport(exchange, chain, validationReport);
    }

    private String extractBody(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Mono<Void> verifyValidationReport(ServerWebExchange exchange, GatewayFilterChain chain,
                                              ValidationReport validationReport) {
        Mono<Void> response;
        if (validationReport.hasErrors()) {
            log.error("Validation report has errors: {}", validationReport);
            ServerHttpResponse serverResponse = exchange.getResponse();
            serverResponse
                    .setStatusCode(HttpStatus.BAD_REQUEST);
            response = serverResponse
                    .writeWith(new Jackson2JsonEncoder().encode(Mono.just(validationReport.getMessages()),
                                                                serverResponse.bufferFactory(),
                                                                ResolvableType.forInstance(validationReport),
                                                                MediaType.APPLICATION_JSON,
                                                                exchange.getAttributes()));
        }
        else {
            log.info("Validation successful");
            response = chain.filter(exchange);
        }
        return response;
    }

    private SimpleRequest.Builder getValidationRequestBuilder(String method, String path, Map<String, String> headers,
                                                              String body) {
        log.debug("Building validation request builder");
        SimpleRequest.Builder builder = switch (method) {
            case "GET" -> SimpleRequest.Builder.get(path);
            case "POST" -> SimpleRequest.Builder.post(path);
            case "PUT" -> SimpleRequest.Builder.put(path);
            case "DELETE" -> SimpleRequest.Builder.delete(path);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
        // TODO 13.05.2025: fix header validation
//        for (Map.Entry<String, String> stringStringEntry : headers.entrySet()) {
//            builder.withHeader(stringStringEntry.getKey(), stringStringEntry.getValue());
//        }
        builder.withBody(body, StandardCharsets.UTF_8);
        return builder;
    }

    public static class Config {

    }
}
