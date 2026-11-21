package com.neo4flix.user_service.dto.response;

import lombok.Data;

@Data 
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private boolean twoFactorEnabled;
    // private boolean enabled;
    private String jwtToken;
    
}