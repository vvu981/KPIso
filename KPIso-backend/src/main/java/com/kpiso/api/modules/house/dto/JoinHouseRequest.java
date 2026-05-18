package com.kpiso.api.modules.house.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinHouseRequest {

    @NotBlank(message = "El código de invitación es obligatorio")
    private String inviteCode;

    @NotNull(message = "El ID del usuario es obligatorio")
    private UUID userId;
}