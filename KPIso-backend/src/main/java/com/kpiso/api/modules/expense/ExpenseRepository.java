package com.kpiso.api.modules.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByHouseIdAndSettledFalseOrderByCreatedAtDesc(UUID houseId);

    // Añadido para solucionar el error de compilación en ExpenseService
    List<Expense> findByHouseIdAndSettledFalse(UUID houseId);

    List<Expense> findByHouseId(UUID houseId);
}