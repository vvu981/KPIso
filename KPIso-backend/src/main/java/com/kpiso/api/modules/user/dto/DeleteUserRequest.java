package com.kpiso.api.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteUserRequest {
    @NotBlank(message = "La contraseña es obligatoria para eliminar la cuenta")
    private String password;
}
