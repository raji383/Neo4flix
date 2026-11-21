package com.neo4flix.api_gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

class FilterTest {

    private static final String JWT_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private final Filter filter = new Filter(new jwtUtil(JWT_SECRET));

    @Test
    void validJwtForwardsSubjectAndRoleInsteadOfClientSuppliedHeaders() {
        String token = Jwts.builder()
                .setSubject("user-123")
                .claim("role", "USER")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET)))
                .compact();
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/movies")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Authenticated-User-Id", "spoofed-user")
                        .header("X-Authenticated-Role", "ADMIN")
                        .build());
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, forwarded -> {
            forwardedExchange.set(forwarded);
            return Mono.empty();
        }).block();

        assertNotNull(forwardedExchange.get());
        assertEquals("user-123", forwardedExchange.get().getRequest()
                .getHeaders().getFirst("X-Authenticated-User-Id"));
        assertEquals("USER", forwardedExchange.get().getRequest()
                .getHeaders().getFirst("X-Authenticated-Role"));
    }

    @Test
    void invalidJwtIsRejectedWithoutCallingTheGatewayChain() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/movies")
                        .header("Authorization", "Bearer invalid-token")
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.filter(exchange, forwarded -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chainCalled.get());
        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }
}
