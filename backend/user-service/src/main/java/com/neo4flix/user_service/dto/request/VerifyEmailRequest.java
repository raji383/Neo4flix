package com.neo4flix.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank
    private String challengeId;

    @NotBlank
    private String otp;
}