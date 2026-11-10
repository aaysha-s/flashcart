package com.flashcart.auth.web;

import com.flashcart.auth.service.AuthService;
import com.flashcart.auth.service.TokenService;
import com.flashcart.auth.user.User;
import com.flashcart.auth.web.dto.LoginRequest;
import com.flashcart.auth.web.dto.RegisterRequest;
import com.flashcart.auth.web.dto.TokenResponse;
import com.flashcart.auth.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request);
        String token = tokenService.createAccessToken(user);
        return ResponseEntity.ok(TokenResponse.bearer(token, tokenService.accessTokenTtlSeconds()));
    }
}