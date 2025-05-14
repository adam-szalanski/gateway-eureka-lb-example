package com.example.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class OpenApiValidationGatewayFilterFactory extends
                                                   AbstractGatewayFilterFactory<OpenApiValidationGatewayFilterFactory.Config> {

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
            return DataBufferUtils.join(request.getBody()
                            .defaultIfEmpty(DefaultDataBufferFactory.sharedInstance.allocateBuffer(0)))
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        String body = new String(bytes, StandardCharsets.UTF_8);

                        return validateRequest(exchange, body, request, method, path, config)
                                .flatMap(valid -> {
                                    if (!valid) {
                                        return exchange.getResponse().setComplete();
                                    }

                                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                            .header(HttpHeaders.CONTENT_LENGTH, Integer.toString(bytes.length))
                                            .build();

                                    ServerWebExchange mutatedExchange = exchange.mutate()
                                            .request(new RequestBodyDecorator(mutatedRequest, exchange, bytes)).build();

                                    return chain.filter(mutatedExchange);
                                });
                    });
        };
    }

    private Mono<Boolean> validateRequest(ServerWebExchange exchange, String body,
                                       ServerHttpRequest request, String method, String path, Config config) {
        Map<String, String> headers = request.getHeaders()
                .toSingleValueMap();
        log.info("Preparing to validate request: method: {}, path: {}, headers: {}, body:{}", method,
                 path, headers,
                 body);
        SimpleRequest.Builder builder = getValidationRequestBuilder(method, path, headers, body);

        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor(new ClassPathResource(config.getSwaggerFilePath()).getPath())
                .build();
        ValidationReport validationReport = validator.validateRequest(
                builder.build());
        log.info("Validation report: {}", validationReport);

        return verifyValidationReport(exchange, validationReport);
    }

    private Mono<Boolean> verifyValidationReport(ServerWebExchange exchange,
                                              ValidationReport validationReport) {
        Mono<Boolean> response;
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
                            exchange.getAttributes()))
                    .thenReturn(false);
        }
        else {
            log.info("Validation successful");
            response = Mono.just(true);
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
        for (Map.Entry<String, String> stringStringEntry : headers.entrySet()) {
            builder.withHeader(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        builder.withBody(body, StandardCharsets.UTF_8);
        return builder;
    }

    @Getter
    @Setter
    public static class Config {
        protected String swaggerFilePath;
    }

    private static class RequestBodyDecorator extends ServerHttpRequestDecorator {
        private final ServerWebExchange exchange;
        private final byte[] bytes;

        RequestBodyDecorator(ServerHttpRequest mutatedRequest, ServerWebExchange exchange, byte[] bytes) {
            super(mutatedRequest);
            this.exchange = exchange;
            this.bytes = bytes;
        }

        @Override
        public Flux<DataBuffer> getBody() {
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);
            return Flux.just(buffer);
        }
    }
}
