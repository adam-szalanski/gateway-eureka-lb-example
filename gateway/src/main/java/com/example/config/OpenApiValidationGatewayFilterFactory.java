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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenApiValidationGatewayFilterFactory extends
                                                   AbstractGatewayFilterFactory<OpenApiValidationGatewayFilterFactory.Config> {

    private static final Jackson2JsonEncoder JSON_ENCODER = new Jackson2JsonEncoder();

    public OpenApiValidationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> DataBufferUtils.join(
                        exchange.getRequest()
                                .getBody()
                                .defaultIfEmpty(DefaultDataBufferFactory.sharedInstance.allocateBuffer(0)))
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    String body = extractRequestBody(dataBuffer, bytes);

                    ValidationReport report = validateRequest(body, exchange.getRequest(), config);

                    if (report.hasErrors()) {
                        log.error("Validation errors: {}", report);
                        return writeErrorResponse(exchange, report);
                    }

                    log.info("Validation successful");
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header(HttpHeaders.CONTENT_LENGTH, Integer.toString(bytes.length))
                            .build();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(new RequestBodyDecorator(mutatedRequest, exchange, bytes)).build();

                    return chain.filter(mutatedExchange);
                });
    }

    private static String extractRequestBody(DataBuffer buffer, byte[] bytes) {
        buffer.read(bytes);
        DataBufferUtils.release(buffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, ValidationReport report) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        response.setStatusCode(status);

        List<String> validationErrorMessageList = report.getMessages()
                .stream()
                .map(ValidationReport.Message::getMessage)
                .toList();
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(status.value(), validationErrorMessageList);

        return response.writeWith(
                JSON_ENCODER.encode(
                        Mono.just(errorResponse),
                        response.bufferFactory(),
                        ResolvableType.forInstance(report),
                        MediaType.APPLICATION_JSON,
                        exchange.getAttributes()));
    }

    private ValidationReport validateRequest(String body, ServerHttpRequest request, Config config) {
        Map<String, String> headers = request.getHeaders().toSingleValueMap();
        String method = request.getMethod().name();
        String path = request.getPath().value();

        log.info("Validating: method={}, path={}, headers={}, body={} ", method, path, headers, body);

        SimpleRequest.Builder builder = buildRequest(method, path, headers, body);
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor(new ClassPathResource(config.getSwaggerFilePath()).getPath())
                .build();

        return validator.validateRequest(builder.build());
    }

    private SimpleRequest.Builder buildRequest(String method, String path, Map<String, String> headers, String body) {
        SimpleRequest.Builder builder = switch (method.toUpperCase()) {
            case "GET" -> SimpleRequest.Builder.get(path);
            case "HEAD" -> SimpleRequest.Builder.head(path);
            case "POST" -> SimpleRequest.Builder.post(path);
            case "PUT" -> SimpleRequest.Builder.put(path);
            case "PATCH" -> SimpleRequest.Builder.patch(path);
            case "DELETE" -> SimpleRequest.Builder.delete(path);
            case "OPTIONS" -> SimpleRequest.Builder.options(path);
            case "TRACE" -> SimpleRequest.Builder.trace(path);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
        headers.forEach(builder::withHeader);
        builder.withBody(body, StandardCharsets.UTF_8);
        return builder;
    }

    @Getter
    @Setter
    public static class Config {
        private String swaggerFilePath;
    }

    private static class RequestBodyDecorator extends ServerHttpRequestDecorator {
        private final ServerWebExchange exchange;
        private final byte[] bytes;

        RequestBodyDecorator(ServerHttpRequest request, ServerWebExchange exchange, byte[] bytes) {
            super(request);
            this.exchange = exchange;
            this.bytes = bytes;
        }

        @Override
        public Flux<DataBuffer> getBody() {
            return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
        }
    }

    private record ValidationErrorResponse(int status, List<String> errors) {
    }

}
