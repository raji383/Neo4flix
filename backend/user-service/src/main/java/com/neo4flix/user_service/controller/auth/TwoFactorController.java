package com.neo4flix.user_service.controller.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.user_service.dto.request.VerifyTwoFactorRequest;
import com.neo4flix.user_service.dto.response.UserResponse;
import com.neo4flix.user_service.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {

    private final AuthService authService;

    public TwoFactorController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/verify")
    public UserResponse verifyTwoFactor(
            @RequestBody @Valid VerifyTwoFactorRequest request) {

        return authService.verifyTwoFactor(request);
    }
}