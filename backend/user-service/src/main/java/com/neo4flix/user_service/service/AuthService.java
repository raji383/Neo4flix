package com.neo4flix.user_service.service;

import org.springframework.stereotype.Service;

import com.neo4flix.user_service.dto.RegisterRequest;
import com.neo4flix.user_service.dto.UserResponse;

@Service
public class AuthService {
    public UserResponse register(RegisterRequest request) {
        // Validate the request, create a new user, and return a UserResponse

        // For example:
        UserResponse response = new UserResponse();
        response.setId("generated-user-id");
        response.setUsername(request.getUsername());
        response.setEmail(request.getEmail());
        response.setJwtToken("generated-jwt-token");
        return response;
    }
}