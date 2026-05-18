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
public class CreateHouseRequest {

    @NotBlank(message = "El nombre de la casa es obligatorio")
    private String name;

    @NotNull(message = "El ID del creador es obligatorio")
    private UUID creatorId;

    private String profilePictureUrl; // Añadido para capturar la URL de la imagen en la creación
}