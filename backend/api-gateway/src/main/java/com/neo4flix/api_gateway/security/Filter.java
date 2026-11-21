package com.neo4flix.api_gateway.security;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

@Component
public class Filter implements GlobalFilter, Ordered {

    private static final byte[] UNAUTHORIZED_BODY = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\",\"details\":{}}"
            .getBytes(StandardCharsets.UTF_8);

    private final jwtUtil jwtUtil;

    public Filter(jwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleanedRequest = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-Authenticated-User-Id");
                    h.remove("X-Authenticated-Role");
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate().request(cleanedRequest).build();

        if (isPublic(sanitizedExchange)) {
            return chain.filter(sanitizedExchange);
        }

        String authorization = sanitizedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(sanitizedExchange);
        }

        try {
            Claims claims = jwtUtil.parseClaims(authorization.substring(7));

            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                return unauthorized(sanitizedExchange);
            }

            Object rolesObj = claims.get("role");
            String roles = rolesObj != null ? rolesObj.toString() : "";

          
            ServerWebExchange authenticatedExchange = sanitizedExchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-Authenticated-User-Id", userId);
                        headers.set("X-Authenticated-Role", roles);
                    }))
                    .build();
            return chain.filter(authenticatedExchange);
        } catch (JwtException | IllegalArgumentException exception) {
            return unauthorized(sanitizedExchange);
        }
    }

    private boolean isPublic(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (method == HttpMethod.OPTIONS) {
            return true;
        }

        if (path.startsWith("/api/auth/")) {
            return true;
        }

        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
            return true;
        }

      

        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(UNAUTHORIZED_BODY);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
