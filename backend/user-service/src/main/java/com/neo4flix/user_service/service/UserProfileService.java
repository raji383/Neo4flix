package com.neo4flix.user_service.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.user_service.dto.request.UpdateProfileRequest;
import com.neo4flix.user_service.dto.response.UserResponse;
import com.neo4flix.user_service.model.User;
import com.neo4flix.user_service.repository.UserRepository;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(String userId) {
        User user = findUser(userId);

        return toResponse(user);
    }

    public UserResponse updateProfile(
            String userId,
            UpdateProfileRequest request) {

        User user = findUser(userId);

        boolean usernameChanged = !user.getUsername().equals(request.username());

        boolean emailChanged = !user.getEmail().equals(request.email());

        if (usernameChanged &&
                userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already taken");
        }

        if (emailChanged &&
                userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        user.setUsername(request.username());
        user.setEmail(request.email());

        try {
            User updatedUser = userRepository.save(user);
            return toResponse(updatedUser);

        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username or email is already in use");
        }
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        // response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        // response.setEnabled(user.isEnabled());
        return response;
    }
}