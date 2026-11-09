package com.flashcart.auth.web.dto;

import com.flashcart.auth.user.Role;
import com.flashcart.auth.user.User;

import java.time.Instant;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}