package com.kpiso.api.modules.expense;

import com.kpiso.api.modules.expense.dto.CreateDirectPaymentRequest;
import com.kpiso.api.modules.expense.dto.DirectPaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/direct-payments")
public class DirectPaymentController {

    private final DirectPaymentService directPaymentService;

    public DirectPaymentController(DirectPaymentService directPaymentService) {
        this.directPaymentService = directPaymentService;
    }

    @PostMapping
    public ResponseEntity<DirectPaymentResponse> createDirectPayment(@Valid @RequestBody CreateDirectPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(directPaymentService.createDirectPayment(request));
    }

    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<DirectPaymentResponse>> getHouseDirectPayments(@PathVariable UUID houseId) {
        return ResponseEntity.ok(directPaymentService.getHouseDirectPayments(houseId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DirectPaymentResponse> updateDirectPayment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDirectPaymentRequest request,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(directPaymentService.updateDirectPayment(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDirectPayment(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        try {
            directPaymentService.deleteDirectPayment(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
