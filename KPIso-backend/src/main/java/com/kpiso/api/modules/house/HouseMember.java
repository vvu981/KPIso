package com.kpiso.api.modules.house;

import com.kpiso.api.modules.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "house_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HouseRole role; // Corregido de String a HouseRole

    @Column(nullable = false, length = 7)
    private String color;

    // Control de ciclo de vida: preserva el historial financiero cuando alguien se
    // marcha
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "settle_approved", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean settleApproved = false;
}