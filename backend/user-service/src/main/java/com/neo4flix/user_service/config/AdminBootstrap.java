package com.neo4flix.user_service.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;

@Component
@ConditionalOnProperty(prefix = "admin.bootstrap", name = "enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.bootstrap.username}") String username,
            @Value("${admin.bootstrap.email}") String email,
            @Value("${admin.bootstrap.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled, but username, email, and password must be configured");
        }

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return;
        }

        User admin = new User();
        admin.setId(UUID.randomUUID().toString());
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole("ADMIN");
        userRepository.save(admin);
    }
}