package com.kpiso.api.modules.house;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseMemberRepository extends JpaRepository<HouseMember, UUID> {
    List<HouseMember> findByHouseId(UUID houseId);

    List<HouseMember> findByUserId(UUID userId);

    boolean existsByHouseIdAndUserId(UUID houseId, UUID userId);
    Optional<HouseMember> findByHouseIdAndUserId(UUID houseId, UUID userId);
}