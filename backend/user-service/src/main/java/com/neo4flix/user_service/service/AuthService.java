package com.neo4flix.user_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
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
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenExpirationMs = tokenExpirationMs;
    }

    // Service methods
    public UserResponse register(RegisterRequest request) {
        ensureUserIsAvailable(request);
        User savedUser = userRepository.save(createUser(request));
        return createUserResponse(savedUser);
    }

    public UserResponse login(LoginRequest request) {
        List<User> matchingUsers = userRepository.findAllByUsername(request.getUsername()).stream()
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .toList();
        if (matchingUsers.isEmpty()) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        if (matchingUsers.size() > 1) {
            throw ApiException.conflict("Multiple accounts match this username and password");
        }
        return createUserResponse(matchingUsers.get(0));
    }

    // Helper methods
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
        return user;
    }

    private UserResponse createUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        Instant issuedAt = Instant.now();
        response.setJwtToken(Jwts.builder()
                .subject(user.getId())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(tokenExpirationMs)))
                .signWith(signingKey)
                .compact());
        return response;
    }
}