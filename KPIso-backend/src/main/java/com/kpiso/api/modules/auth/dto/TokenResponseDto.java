package com.kpiso.api.modules.auth.dto;

import lombok.*;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class TokenResponseDto {
    private final String accessToken;
    private final String refreshToken;
    private final UUID userId;
    private final String username;
    private final String email;
    private final String profilePictureUrl;
}