package com.neo4flix.user_service.config;


import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;

@Configuration 
public class AdminSeeder {

    @Bean 
    public CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                
                User admin = new User();
                admin.setId(UUID.randomUUID().toString());
                admin.setUsername("admin");
                admin.setEmail("admin@blog.com");
                admin.setPasswordHash(passwordEncoder.encode("admin123")); 
                admin.setRole("ADMIN");
                userRepository.save(admin);
            }
        };
    }
}