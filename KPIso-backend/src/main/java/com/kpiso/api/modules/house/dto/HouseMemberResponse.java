package com.kpiso.api.modules.house.dto;

import com.kpiso.api.modules.house.HouseRole;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseMemberResponse {
    private UUID userId;
    private String username;
    private String profilePictureUrl;
    private HouseRole role;
}