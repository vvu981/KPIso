package com.kpiso.api.modules.auth;

import com.kpiso.api.modules.auth.dto.LoginRequest;
import com.kpiso.api.modules.auth.dto.LoginResponse;
import com.kpiso.api.modules.auth.dto.RegisterRequest;
import com.kpiso.api.modules.auth.dto.TokenResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuario registrado correctamente");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponseDto tokens = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .maxAge(7L * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        LoginResponse responseBody = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .userId(tokens.getUserId())
                .username(tokens.getUsername())
                .email(tokens.getEmail())
                .profilePictureUrl(tokens.getProfilePictureUrl())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseBody);
    }
}