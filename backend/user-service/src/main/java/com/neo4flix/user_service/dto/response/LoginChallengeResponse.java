package com.neo4flix.user_service.dto.response;

import lombok.Data;

@Data
public class LoginChallengeResponse {
    private String challengeId;
    private boolean requiresTwoFactor;
    private String id;
    private String username;
    private String email;
    private String role;
    private String jwtToken;

    public LoginChallengeResponse(String challengeId) {
        this.challengeId = challengeId;
        this.requiresTwoFactor = true;
    }

    public LoginChallengeResponse(UserResponse user) {
        this.requiresTwoFactor = false;
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.jwtToken = user.getJwtToken();
    }
}