package com.flashcart.auth.web;

import com.flashcart.auth.service.AuthService;
import com.flashcart.auth.service.TokenService;
import com.flashcart.auth.token.RefreshTokenService;
import com.flashcart.auth.token.RotatedTokens;
import com.flashcart.auth.user.User;
import com.flashcart.auth.web.dto.LoginRequest;
import com.flashcart.auth.web.dto.RefreshRequest;
import com.flashcart.auth.web.dto.RegisterRequest;
import com.flashcart.auth.web.dto.TokenResponse;
import com.flashcart.auth.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService,
                          TokenService tokenService,
                          RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request);
        return ResponseEntity.ok(issueTokens(user, refreshTokenService.create(user)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RotatedTokens rotated = refreshTokenService.rotate(request.refreshToken());
        return ResponseEntity.ok(issueTokens(rotated.user(), rotated.refreshToken()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return UserResponse.from(authService.getUser(userId));
    }

    private TokenResponse issueTokens(User user, String refreshToken) {
        String accessToken = tokenService.createAccessToken(user);
        return TokenResponse.bearer(accessToken, refreshToken, tokenService.accessTokenTtlSeconds());
    }
}