package com.neo4flix.user_service.controller.user;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.neo4flix.user_service.dto.request.UpdateProfileRequest;
import com.neo4flix.user_service.dto.response.UserResponse;
import com.neo4flix.user_service.service.UserProfileService;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserResponse getProfile(
            @RequestHeader("X-Authenticated-User-Id") String userId) {

        return userProfileService.getProfile(userId);
    }

    @PutMapping
    public UserResponse updateProfile(
            @RequestHeader("X-Authenticated-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        return userProfileService.updateProfile(userId, request);
    }
}