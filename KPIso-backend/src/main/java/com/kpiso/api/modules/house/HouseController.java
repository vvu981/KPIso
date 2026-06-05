package com.kpiso.api.modules.house;

import com.kpiso.api.modules.house.dto.CreateHouseRequest;
import com.kpiso.api.modules.house.dto.HouseDetailResponse;
import com.kpiso.api.modules.house.stats.dto.HouseStatsResponse;
import com.kpiso.api.modules.house.stats.HouseStatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.YearMonth;
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

    private final HouseStatisticsService houseStatisticsService;

    private final HouseService houseService;

    public HouseController(HouseService houseService, HouseStatisticsService houseStatisticsService) {
        this.houseService = houseService;
        this.houseStatisticsService = houseStatisticsService;
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
    public ResponseEntity<HouseDetailResponse> getHouseDetail(@PathVariable UUID houseId, @RequestParam UUID userId) {
        try {
            HouseDetailResponse houseDetail = houseService.getHouseDetail(houseId, userId);
            return ResponseEntity.ok(houseDetail);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    @PreAuthorize("hasRole('HOUSE_MEMBER')")
    @GetMapping("/{houseId}/stats")
    public ResponseEntity<HouseStatsResponse> getHouseStats(@PathVariable UUID houseId,
                                                             @RequestParam(required = false) String month,
                                                             @RequestParam UUID userId) {
        try {
            // month=null → histórico total para evolución; YearMonth.now() como fallback para KPI
            YearMonth ym = (month != null && !month.isBlank()) ? YearMonth.parse(month) : null;

            HouseStatsResponse response = new HouseStatsResponse();
            response.setLivingCostPerMember(houseStatisticsService.getLivingCostPerMember(houseId, userId, ym));
            response.setMonthlyExpenseEvolution(houseStatisticsService.getMonthlyExpenseEvolution(houseId, userId));
            response.setTopExpenses(houseStatisticsService.getTopExpenses(houseId, userId, 10, ym));
            response.setProductStats(houseStatisticsService.getProductPurchaseStats(houseId, userId));
            response.setTaskKpiPoints(houseStatisticsService.getTaskKpiPoints(houseId, userId, ym));
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

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