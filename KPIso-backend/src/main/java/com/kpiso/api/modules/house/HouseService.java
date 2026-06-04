package com.kpiso.api.modules.house;

import com.kpiso.api.modules.house.dto.CreateHouseRequest;
import com.kpiso.api.modules.house.dto.HouseDetailResponse;
import com.kpiso.api.modules.house.dto.UserHouseResponse;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import com.kpiso.api.modules.expense.Expense;
import com.kpiso.api.modules.expense.ExpenseRepository;
import com.kpiso.api.modules.activity.ActivityLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class HouseService {

        private final HouseRepository houseRepository;
        private final HouseMemberRepository houseMemberRepository;
        private final UserRepository userRepository;
        private final ActivityLogService activityLogService;
        private final ExpenseRepository expenseRepository; // Inyectado para validar saldos antes de expulsiones

        public HouseService(HouseRepository houseRepository,
                        HouseMemberRepository houseMemberRepository,
                        UserRepository userRepository,
                        ActivityLogService activityLogService,
                        ExpenseRepository expenseRepository) {
                this.houseRepository = houseRepository;
                this.houseMemberRepository = houseMemberRepository;
                this.userRepository = userRepository;
                this.activityLogService = activityLogService;
                this.expenseRepository = expenseRepository;
        }

        @Transactional
        public House createHouse(CreateHouseRequest request) {
                User creator = userRepository.findById(request.getCreatorId())
                                .orElseThrow(() -> new IllegalArgumentException("El usuario creador no existe"));

                House house = House.builder()
                                .name(request.getName())
                                .inviteCode(generateUniqueInviteCode())
                                .profilePictureUrl(request.getProfilePictureUrl())
                                .build();

                House savedHouse = houseRepository.save(house);

                HouseMember member = HouseMember.builder()
                                .house(savedHouse)
                                .user(creator)
                                .role(HouseRole.ADMIN)
                                .color("#6366f1")
                                .active(true)
                                .build();

                houseMemberRepository.save(member);
                return savedHouse;
        }

        @Transactional
        public House joinHouse(String inviteCode, UUID userId) {
                House house = houseRepository.findByInviteCode(inviteCode)
                                .orElseThrow(() -> new IllegalArgumentException("El código no existe"));

                if (house.getDeletedAt() != null) {
                        throw new IllegalStateException("No puedes unirte a una casa que ha sido eliminada");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

                Optional<HouseMember> existingMemberOpt = houseMemberRepository.findByHouseIdAndUserId(house.getId(),
                                userId);

                if (existingMemberOpt.isPresent()) {
                        HouseMember existingMember = existingMemberOpt.get();
                        if (existingMember.isActive()) {
                                throw new IllegalStateException("Ya eres miembro de esta vivienda");
                        } else {
                                // Si vuelve al piso, lo reactivamos para mantener su línea temporal
                                existingMember.setActive(true);
                                existingMember.setRole(HouseRole.MEMBER);
                                houseMemberRepository.save(existingMember);
                                return house;
                        }
                }

                HouseMember member = HouseMember.builder()
                                .house(house)
                                .user(user)
                                .role(HouseRole.MEMBER)
                                .color("#10b981")
                                .active(true)
                                .build();

                houseMemberRepository.save(member);
                return house;
        }

        @Transactional(readOnly = true)
        public HouseDetailResponse getHouseDetail(UUID houseId) {
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> new IllegalArgumentException("La casa no existe"));

                List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);

                // Excluimos a los inactivos de la vista principal de convivencia
                List<HouseDetailResponse.HouseMemberResponse> memberDtos = members.stream()
                                .filter(HouseMember::isActive)
                                .map(m -> HouseDetailResponse.HouseMemberResponse.builder()
                                                .userId(m.getUser().getId())
                                                .username(m.getUser().getUsername())
                                                .role(m.getRole().name())
                                                .settleApproved(m.isSettleApproved())
                                                .build())
                                .collect(Collectors.toList());

                return HouseDetailResponse.builder()
                                .id(house.getId())
                                .name(house.getName())
                                .inviteCode(house.getInviteCode())
                                .profilePictureUrl(house.getProfilePictureUrl())
                                .members(memberDtos)
                                .isReadOnly(house.getDeletedAt() != null)
                                .build();
        }

        @Transactional(readOnly = true)
        public HouseDetailResponse getHouseDetail(UUID houseId, UUID userId) {
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> new IllegalArgumentException("La casa no existe"));

                HouseMember requester = houseMemberRepository.findByHouseIdAndUserId(houseId, userId)
                                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No tienes permiso para ver esta casa"));

                List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);

                List<HouseDetailResponse.HouseMemberResponse> memberDtos = members.stream()
                                .filter(HouseMember::isActive)
                                .map(m -> HouseDetailResponse.HouseMemberResponse.builder()
                                                .userId(m.getUser().getId())
                                                .username(m.getUser().getUsername())
                                                .role(m.getRole().name())
                                                .settleApproved(m.isSettleApproved())
                                                .build())
                                .collect(Collectors.toList());

                boolean isReadOnly = !requester.isActive() || house.getDeletedAt() != null;

                return HouseDetailResponse.builder()
                                .id(house.getId())
                                .name(house.getName())
                                .inviteCode(house.getInviteCode())
                                .profilePictureUrl(house.getProfilePictureUrl())
                                .members(memberDtos)
                                .isReadOnly(isReadOnly)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<UserHouseResponse> getUserHouses(UUID userId) {
                return houseMemberRepository.findByUserId(userId).stream()
                                .filter(m -> m.getHouse().getDeletedAt() == null && m.isActive())
                                .map(m -> mapToUserHouseResponse(m.getHouse()))
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<UserHouseResponse> getDeletedUserHouses(UUID userId) {
                return houseMemberRepository.findByUserId(userId).stream()
                                .filter(m -> m.getHouse().getDeletedAt() != null || !m.isActive())
                                .map(m -> mapToUserHouseResponse(m.getHouse()))
                                .collect(Collectors.toList());
        }

        @Transactional
        public void softDeleteHouse(UUID houseId, UUID userId) {
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> new IllegalArgumentException("Casa no encontrada"));

                HouseMember member = houseMemberRepository.findByHouseIdAndUserId(houseId, userId)
                                .orElseThrow(() -> new IllegalArgumentException("No perteneces a esta casa"));

                if (member.getRole() != HouseRole.ADMIN) {
                        throw new IllegalStateException("Solo los administradores pueden eliminar la vivienda");
                }

                house.setDeletedAt(LocalDateTime.now());
                houseRepository.save(house);
        }

        @Transactional
        public HouseDetailResponse updateHouse(UUID houseId, CreateHouseRequest request, UUID requestingUserId) {
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> new IllegalArgumentException("La casa especificada no existe"));

                User actor = userRepository.findById(requestingUserId)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario solicitante inválido"));

                houseMemberRepository.findByHouseIdAndUserId(houseId, requestingUserId)
                                .orElseThrow(() -> new IllegalArgumentException("Acceso denegado"));

                house.setName(request.getName());
                house.setProfilePictureUrl(request.getProfilePictureUrl());
                houseRepository.save(house);

                String msg = String.format("%s modificó el perfil del hogar: Nombre '%s'", actor.getUsername(),
                                house.getName());
                activityLogService.log(msg, "UPDATE", house, actor);

                return getHouseDetail(houseId, requestingUserId);
        }

        // MODIFICADO CON REGLA SOLID: Bloqueo estricto si el balance financiero no es
        // exactamente 0.00€
        @Transactional
        public void removeMember(UUID houseId, UUID targetUserId, UUID requestingUserId) {
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> new IllegalArgumentException("La casa especificada no existe"));

                User actor = userRepository.findById(requestingUserId)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario ejecutor inválido"));

                User targetUser = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new IllegalArgumentException("La persona a eliminar no existe"));

                HouseMember requesterMember = houseMemberRepository.findByHouseIdAndUserId(houseId, requestingUserId)
                                .orElseThrow(() -> new IllegalArgumentException("No perteneces a esta vivienda"));

                if (requesterMember.getRole() != HouseRole.ADMIN) {
                        throw new IllegalStateException(
                                        "Acceso denegado: Solo los administradores pueden expulsar personas");
                }

                // ALGORITMO: Calcular el balance neto exacto del usuario objetivo antes de
                // proceder
                List<Expense> expenses = expenseRepository.findByHouseIdAndSettledFalse(houseId);
                BigDecimal targetBalance = BigDecimal.ZERO;

                for (Expense e : expenses) {
                        if (e.getPaidBy().getId().equals(targetUserId)) {
                                targetBalance = targetBalance.add(e.getAmount());
                        }
                        
                        Map<UUID, BigDecimal> splits = new HashMap<>();
                        if (e.getExactSplits() != null && !e.getExactSplits().isEmpty()) {
                                splits = e.getExactSplits();
                        } else {
                                List<User> participants = e.getParticipants();
                                int numParticipants = participants.size();
                                if (numParticipants > 0) {
                                        long totalCents = e.getAmount().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                                        long baseShareCents = totalCents / numParticipants;
                                        long remainderCents = totalCents % numParticipants;

                                        for (int i = 0; i < numParticipants; i++) {
                                                long cents = baseShareCents + (i < remainderCents ? 1 : 0);
                                                BigDecimal share = BigDecimal.valueOf(cents).movePointLeft(2);
                                                splits.put(participants.get(i).getId(), share);
                                        }
                                }
                        }

                        if (splits.containsKey(targetUserId)) {
                                targetBalance = targetBalance.subtract(splits.get(targetUserId));
                        }
                }

                // Validación de balance financiero: si tiene deudas o saldos a favor
                // (tolerancia de 5 céntimos, para coincidir con ExpenseService.getHouseMemberStatuses)
                if (targetBalance.abs().compareTo(new BigDecimal("0.05")) > 0) {
                        throw new IllegalStateException(String.format(
                                        "No se puede expulsar a %s porque su cuenta no está liquidada. Debe dejar su saldo en 0.00€ (Saldo actual: %s€)",
                                        targetUser.getUsername(), targetBalance.setScale(2, RoundingMode.HALF_UP)));
                }

                HouseMember targetMember = houseMemberRepository.findByHouseIdAndUserId(houseId, targetUserId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "La persona seleccionada no pertenece a esta casa"));

                // Se desactiva la participación futura sin destruir el historial financiero
                targetMember.setActive(false);
                houseMemberRepository.save(targetMember);

                String msg = String.format("%s revocó el acceso al piso y expulsó a '%s'", actor.getUsername(),
                                targetUser.getUsername());
                activityLogService.log(msg, "DELETE", house, actor);
        }

        private UserHouseResponse mapToUserHouseResponse(House house) {
                return UserHouseResponse.builder()
                                .id(house.getId())
                                .name(house.getName())
                                .inviteCode(house.getInviteCode())
                                .profilePictureUrl(house.getProfilePictureUrl())
                                .build();
        }

        private String generateUniqueInviteCode() {
                String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                Random random = new Random();
                String code;
                do {
                        StringBuilder sb = new StringBuilder(6);
                        for (int i = 0; i < 6; i++)
                                sb.append(characters.charAt(random.nextInt(characters.length())));
                        code = sb.toString();
                } while (houseRepository.findByInviteCode(code).isPresent());
                return code;
        }
}