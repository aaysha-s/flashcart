package com.flashcart.auth.service;

import com.flashcart.auth.user.Role;
import com.flashcart.auth.user.User;
import com.flashcart.auth.user.UserRepository;
import com.flashcart.auth.web.dto.LoginRequest;
import com.flashcart.auth.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    @Test
    void register_savesNewCustomerWithHashedPassword() {
        when(userRepository.existsByEmail("aaysha@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User user = authService.register(new RegisterRequest("  Aaysha@Test.com ", "password123"));

        assertThat(user.getEmail()).isEqualTo("aaysha@test.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("aaysha@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("aaysha@test.com", "password123")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = new User("aaysha@test.com", "hashed", Role.CUSTOMER);
        when(userRepository.findByEmail("aaysha@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("aaysha@test.com", "wrongpass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@test.com", "anything")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
