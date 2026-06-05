package com.kpiso.api.modules.auth;

import com.kpiso.api.modules.auth.dto.LoginRequest;
import com.kpiso.api.modules.auth.dto.RegisterRequest;
import com.kpiso.api.modules.auth.dto.TokenResponseDto;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerShouldSaveUser() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@email.com")
                .username("testuser")
                .password("password123")
                .profilePictureUrl("http://pic.jpg")
                .build();

        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        authService.register(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldFailIfEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@email.com")
                .username("testuser")
                .build();

        when(userRepository.existsByEmail("test@email.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void registerShouldFailIfUsernameExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@email.com")
                .username("testuser")
                .build();

        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void loginShouldReturnTokensOnValidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("test@email.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@email.com")
                .username("testuser")
                .password("encodedPassword")
                .profilePictureUrl("http://pic.jpg")
                .build();

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken("testuser")).thenReturn("access_token");
        when(jwtService.generateRefreshToken("testuser")).thenReturn("refresh_token");

        TokenResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(user.getId(), response.getUserId());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void loginShouldFailWhenUserNotFound() {
        LoginRequest request = LoginRequest.builder()
                .email("test@email.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void loginShouldFailWhenPasswordIncorrect() {
        LoginRequest request = LoginRequest.builder()
                .email("test@email.com")
                .password("password123")
                .build();

        User user = User.builder()
                .email("test@email.com")
                .password("encodedPassword")
                .build();

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
}
