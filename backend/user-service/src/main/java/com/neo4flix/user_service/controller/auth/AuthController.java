package com.neo4flix.user_service.controller.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.user_service.dto.RegisterRequest;
import com.neo4flix.user_service.dto.UserResponse;
import com.neo4flix.user_service.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse register(@RequestBody @Valid RegisterRequest request) {
        System.out.println("Register request received: " + request);
        return authService.register(request);
    }
}