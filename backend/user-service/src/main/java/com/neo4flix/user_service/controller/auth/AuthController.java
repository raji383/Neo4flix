package com.neo4flix.user_service.controller.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/auth")
public class AuthController {
    @PostMapping("/register")
    public String register() {
        return "User registered successfully!";
    }
}