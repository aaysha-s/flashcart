package com.flashcart.auth.service;

import com.flashcart.auth.user.Role;
import com.flashcart.auth.user.User;
import com.flashcart.auth.user.UserRepository;
import com.flashcart.auth.web.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        String hash = passwordEncoder.encode(request.password());
        return userRepository.save(new User(email, hash, Role.CUSTOMER));
    }
}
