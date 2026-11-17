package com.neo4flix.api_gateway.security;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

public class JwtAuthenticationFilter implements WebFilter {

    private static final String USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String USER_ROLE_HEADER = "X-Authenticated-User-Role";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        var request = exchange.getRequest();

        // Always remove a client-supplied identity header.
        var sanitizedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                })
                .build();

        var sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        // Allow CORS preflight requests.
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(sanitizedExchange);
        }

        // Public authentication endpoints.
        if (isPublicEndpoint(request)) {
            return chain.filter(sanitizedExchange);
        }

        String authorization = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null ||
                !authorization.startsWith("Bearer ") ||
                authorization.substring(7).isBlank()) {
            return unauthorized(exchange);
        }

        String token = authorization.substring(7).trim();

        try {
            JwtService.Identity identity = jwtService.validateAndExtractIdentity(token);

            var authenticatedRequest = sanitizedRequest.mutate()
                    .header(USER_ID_HEADER, identity.userId())
                    .header(USER_ROLE_HEADER, identity.role())
                    .build();

            var authenticatedExchange = sanitizedExchange.mutate()
                    .request(authenticatedRequest)
                    .build();

            return chain.filter(authenticatedExchange);

        } catch (Exception ex) {
            return unauthorized(exchange);
        }
    }

    private boolean isPublicEndpoint(ServerHttpRequest request) {

        String path = request.getPath().value();

        return (request.getMethod() == HttpMethod.POST &&
            (path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/users/me")))
            || (request.getMethod() == HttpMethod.GET
                && path.equals("/actuator/health"));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"status":401,"error":"Unauthorized",
                 "message":"Missing or invalid access token"}
                """;

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}