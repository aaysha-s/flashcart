package com.flashcart.auth.token;

import com.flashcart.auth.user.User;

public record RotatedTokens(User user, String refreshToken) {}