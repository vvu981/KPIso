package com.kpiso.api.modules.house;

import com.kpiso.api.modules.house.dto.CreateHouseRequest;
import com.kpiso.api.modules.house.dto.HouseDetailResponse;
import com.kpiso.api.modules.house.dto.UserHouseResponse;
import com.kpiso.api.modules.house.dto.JoinHouseRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/houses")
public class HouseController {

    private final HouseService houseService;

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @PostMapping
    public ResponseEntity<?> createHouse(@Valid @RequestBody CreateHouseRequest request) {
        try {
            House house = houseService.createHouse(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(house);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinHouse(@Valid @RequestBody JoinHouseRequest request) {
        try {
            House house = houseService.joinHouse(request.getInviteCode(), request.getUserId());
            return ResponseEntity.ok(house);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{houseId}")
    public ResponseEntity<HouseDetailResponse> getHouseDetail(@PathVariable UUID houseId) {
        try {
            HouseDetailResponse houseDetail = houseService.getHouseDetail(houseId);
            return ResponseEntity.ok(houseDetail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserHouseResponse>> getUserHouses(@PathVariable UUID userId) {
        return ResponseEntity.ok(houseService.getUserHouses(userId));
    }

    @GetMapping("/user/{userId}/deleted")
    public ResponseEntity<List<UserHouseResponse>> getDeletedHouses(@PathVariable UUID userId) {
        return ResponseEntity.ok(houseService.getDeletedUserHouses(userId));
    }

    @DeleteMapping("/{houseId}")
    public ResponseEntity<?> deleteHouse(@PathVariable UUID houseId, @RequestParam UUID userId) {
        try {
            houseService.softDeleteHouse(houseId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // NUEVO ENDPOINT: Modificación de nombre e imagen de la vivienda
    @PutMapping("/{houseId}")
    public ResponseEntity<?> updateHouse(@PathVariable UUID houseId, @Valid @RequestBody CreateHouseRequest request, @RequestParam UUID userId) {
        try {
            HouseDetailResponse updated = houseService.updateHouse(houseId, request, userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{houseId}/members/{memberUserId}")
    public ResponseEntity<?> removeMember(@PathVariable UUID houseId, @PathVariable UUID memberUserId, @RequestParam UUID userId) {
        try {
            houseService.removeMember(houseId, memberUserId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}