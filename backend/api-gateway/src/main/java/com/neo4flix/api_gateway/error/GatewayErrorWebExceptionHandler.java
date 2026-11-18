package com.neo4flix.api_gateway.error;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode status = resolveStatus(ex);
        byte[] body = responseBody(codeFor(status), messageFor(status));

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private HttpStatusCode resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        if (ex instanceof ConnectException || ex.getCause() instanceof ConnectException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 429 -> "TOO_MANY_REQUESTS";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "REQUEST_FAILED";
        };
    }

    private String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Malformed or incomplete request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "The requested endpoint does not exist";
            case 409 -> "The request conflicts with the current resource state";
            case 405 -> "Method not supported for this endpoint";
            case 413 -> "Request payload is too large";
            case 415 -> "Content type not supported";
            case 429 -> "Too many requests";
            case 503 -> "A required service is unavailable";
            default -> status.is5xxServerError()
                    ? "An unexpected error occurred"
                    : "The request could not be processed";
        };
    }

    private byte[] responseBody(String code, String message) {
        return ("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"details\":{}}")
                .getBytes(StandardCharsets.UTF_8);
    }
}
