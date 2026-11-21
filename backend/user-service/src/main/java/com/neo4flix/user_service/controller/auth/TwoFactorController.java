package com.neo4flix.user_service.controller.auth;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.user_service.dto.request.VerifyEmailRequest;
import com.neo4flix.user_service.dto.request.VerifyTwoFactorRequest;
import com.neo4flix.user_service.dto.response.UserResponse;
import com.neo4flix.user_service.service.AuthService;
import com.neo4flix.user_service.service.EmailVerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {

    private final AuthService authService;

    private final EmailVerificationService emailVerificationService;

    public TwoFactorController(
            AuthService authService,
            EmailVerificationService emailVerificationService) {

        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/verify")
    public UserResponse verifyTwoFactor(
            @RequestBody @Valid VerifyTwoFactorRequest request) {

        return authService.verifyTwoFactor(request);
    }

    @PostMapping("/email/verify")
    @ResponseStatus(HttpStatus.OK)
    public void verifyEmail(
            @RequestBody @Valid VerifyEmailRequest request) {

        emailVerificationService.verifyEmail(
                request.getChallengeId(),
                request.getOtp());
    }
}