package com.neo4flix.user_service.dto;

import lombok.Data;

@Data 
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    // private boolean enabled;
    private String jwtToken;
    
}