package com.kpiso.api.modules.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DirectPaymentRepository extends JpaRepository<DirectPayment, UUID> {
    List<DirectPayment> findByHouseIdAndSettledFalse(UUID houseId);
    List<DirectPayment> findByHouseId(UUID houseId);
}
