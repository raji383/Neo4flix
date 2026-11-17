package com.neo4flix.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret));
    }

    public Identity validateAndExtractIdentity(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("JWT role is missing");
        }

        return new Identity(userId, role);
    }

    public record Identity(String userId, String role) {}
}