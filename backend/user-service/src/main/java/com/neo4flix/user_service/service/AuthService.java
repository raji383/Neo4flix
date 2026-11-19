package com.neo4flix.user_service.service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.neo4flix.user_service.dto.LoginRequest;
import com.neo4flix.user_service.dto.RegisterRequest;
import com.neo4flix.user_service.dto.UserResponse;
import com.neo4flix.user_service.exception.ApiException;
import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey signingKey;
    private final long tokenExpirationMs;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration:3600000}") long tokenExpirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret));
        this.tokenExpirationMs = tokenExpirationMs;
    }

    // Service methods
    public UserResponse register(RegisterRequest request) {
        ensurePasswordsMatch(request);
        ensureUserIsAvailable(request);
        User savedUser = userRepository.save(createUser(request));
        return createUserResponse(savedUser);
    }

    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        return createUserResponse(user);
    }

    // Helper methods
    private void ensurePasswordsMatch(RegisterRequest request) {
        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw ApiException.badRequest("Passwords do not match");
        }
    }

    private void ensureUserIsAvailable(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw ApiException.conflict("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email already exists");
        }
    }

    private User createUser(RegisterRequest request) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        return user;
    }

    private UserResponse createUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        Instant issuedAt = Instant.now();
        response.setJwtToken(Jwts.builder()
                .subject(user.getId())
                .claim("role", user.getRole())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(tokenExpirationMs)))
                .signWith(signingKey)
                .compact());
        return response;
    }
}
