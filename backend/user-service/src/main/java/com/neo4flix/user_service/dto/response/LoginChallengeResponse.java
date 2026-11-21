package com.neo4flix.user_service.dto.response;

import lombok.Data;

@Data
public class LoginChallengeResponse {
    private String challengeId;
    private boolean requiresTwoFactor;

    public LoginChallengeResponse(String challengeId) {
        this.challengeId = challengeId;
        this.requiresTwoFactor = true;
    }
}