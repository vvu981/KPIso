package com.kpiso.api.modules.house.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHouseResponse {
    private UUID id;
    private String name;
    private String inviteCode;
    private String profilePictureUrl; // Añadido para que se vea en el Dashboard
}