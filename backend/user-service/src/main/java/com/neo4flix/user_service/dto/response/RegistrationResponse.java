package com.neo4flix.user_service.dto.response;

import lombok.Data;

@Data
public class RegistrationResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private String message;
}