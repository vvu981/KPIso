package com.kpiso.api.modules.house.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseDetailResponse {
    private UUID id;
    private String name;
    private String inviteCode;
    private String profilePictureUrl;
    private List<HouseMemberResponse> members;
    private Boolean isReadOnly;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HouseMemberResponse {
        private UUID userId;
        private String username;
        private String role;
    }
}