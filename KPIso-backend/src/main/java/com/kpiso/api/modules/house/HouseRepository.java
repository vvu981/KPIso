package com.kpiso.api.modules.house;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface HouseRepository extends JpaRepository<House, UUID> {
    Optional<House> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}