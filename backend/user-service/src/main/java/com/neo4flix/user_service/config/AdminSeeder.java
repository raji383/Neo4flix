package com.neo4flix.user_service.config;


import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;

@Configuration 
public class AdminSeeder {

    @Bean 
    public CommandLineRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.bootstrap.enabled:false}") boolean enabled,
            @Value("${admin.bootstrap.username:admin}") String username,
            @Value("${admin.bootstrap.email:admin@neo4flix.local}") String email,
            @Value("${admin.bootstrap.password:}") String password,
            @Value("${admin.bootstrap.user-id:}") String userId) {
        return args -> {
            if (!enabled) {
                return;
            }

            var existingAdmin = userRepository.findByUsername(username);
            if (existingAdmin.isPresent()) {
                if (!"ADMIN".equalsIgnoreCase(existingAdmin.get().getRole())) {
                    throw new IllegalStateException(
                            "Configured admin username belongs to a non-admin user"
                    );
                }
                if (!userId.isBlank() && !userId.equals(existingAdmin.get().getId())) {
                    throw new IllegalStateException(
                            "ADMIN_USER_ID does not match the existing admin user's ID"
                    );
                }
                return;
            }

            if (!userId.isBlank() && userRepository.findById(userId).isPresent()) {
                throw new IllegalStateException(
                        "ADMIN_USER_ID is already assigned to another user"
                );
            }

            if (password.isBlank()) {
                throw new IllegalStateException(
                        "Configure ADMIN_PASSWORD when admin bootstrap is enabled"
                );
            }

            User admin = new User();
            admin.setId(userId.isBlank() ? UUID.randomUUID().toString() : userId);
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole("ADMIN");
            userRepository.save(admin);
        };
    }
}