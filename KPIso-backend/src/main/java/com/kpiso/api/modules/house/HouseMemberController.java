package com.kpiso.api.modules.house;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/houses/{houseId}/members")
public class HouseMemberController {

    private final HouseMemberRepository houseMemberRepository;

    public HouseMemberController(HouseMemberRepository houseMemberRepository) {
        this.houseMemberRepository = houseMemberRepository;
    }

    @PatchMapping("/color")
    public ResponseEntity<?> updateMemberColor(@PathVariable UUID houseId, @RequestParam UUID userId, @RequestParam String color) {
        return houseMemberRepository.findByHouseIdAndUserId(houseId, userId)
                .map(member -> {
                    // Validación de seguridad: Asegurar que sea un formato hexadecimal válido
                    if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
                        return ResponseEntity.badRequest().body("Formato de color inválido. Debe usar formato #RRGGBB");
                    }
                    member.setColor(color);
                    houseMemberRepository.save(member);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}