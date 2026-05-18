package com.kpiso.api.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    private String username;

    @NotBlank(message = "El correo no puede estar vacío")
    private String email;

    private String profilePictureUrl;

    private String password;

    private String currentPassword;
}