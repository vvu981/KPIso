package com.kpiso.api.modules.expense;

import com.kpiso.api.modules.expense.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(request));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable UUID expenseId, @Valid @RequestBody CreateExpenseRequest request, @RequestParam UUID userId) {
        return ResponseEntity.ok(expenseService.updateExpense(expenseId, request, userId));
    }

    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<ExpenseResponse>> getHouseExpenses(@PathVariable UUID houseId) {
        return ResponseEntity.ok(expenseService.getHouseExpenses(houseId));
    }

    // NUEVO ENDPOINT: Expone los estados de balances y colores cruzados del piso
    @GetMapping("/house/{houseId}/statuses")
    public ResponseEntity<Map<UUID, MemberStatusResponse>> getHouseMemberStatuses(@PathVariable UUID houseId) {
        return ResponseEntity.ok(expenseService.getHouseMemberStatuses(houseId));
    }

    @GetMapping("/house/{houseId}/settlement")
    public ResponseEntity<List<DebtSettlementResponse>> getSettlements(@PathVariable UUID houseId) {
        return ResponseEntity.ok(expenseService.calculateSettlements(houseId));
    }

    @PostMapping("/house/{houseId}/settle-all")
    public ResponseEntity<?> settleAll(@PathVariable UUID houseId, @RequestParam UUID userId) {
        try {
            expenseService.settleAllHouseExpenses(houseId, userId);
            return ResponseEntity.ok("Cuentas del piso liquidadas.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable UUID expenseId, @RequestParam UUID userId) {
        try {
            expenseService.deleteExpense(expenseId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}