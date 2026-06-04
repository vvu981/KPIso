package com.kpiso.api.modules.house;

import com.kpiso.api.modules.activity.ActivityLogService;
import com.kpiso.api.modules.expense.Expense;
import com.kpiso.api.modules.expense.ExpenseRepository;
import com.kpiso.api.modules.house.dto.CreateHouseRequest;
import com.kpiso.api.modules.house.dto.HouseDetailResponse;
import com.kpiso.api.modules.house.dto.UserHouseResponse;
import com.kpiso.api.modules.task.Task;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import com.kpiso.api.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private HouseMemberRepository houseMemberRepository;

    @Mock
    private UserRepository userRepository;

    private ActivityLogService activityLogService;

    @Mock
    private ExpenseRepository expenseRepository;

    private HouseService houseService;

    private User creator;
    private User guest;
    private House house;

    @BeforeEach
    void setUp() {
        creator = TestFixtures.user("creator", "creator@email.com");
        guest = TestFixtures.user("guest", "guest@email.com");
        house = TestFixtures.house("Mi Piso", "ABC123");
        activityLogService = new ActivityLogService(null) {
            @Override
            public void log(String description, String actionType, House house, User user) {
            }
        };
        houseService = new HouseService(houseRepository, houseMemberRepository, userRepository, activityLogService, expenseRepository);
    }

    @Test
    void createHouseShouldPersistHouseAndAdminMember() {
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Mi Piso")
                .creatorId(creator.getId())
                .profilePictureUrl("https://cdn.example.com/house.png")
                .build();

        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(houseRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.save(any(HouseMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        House savedHouse = houseService.createHouse(request);

        assertEquals("Mi Piso", savedHouse.getName());
        assertEquals("https://cdn.example.com/house.png", savedHouse.getProfilePictureUrl());
        assertNotNull(savedHouse.getInviteCode());
        assertEquals(6, savedHouse.getInviteCode().length());

        ArgumentCaptor<HouseMember> memberCaptor = ArgumentCaptor.forClass(HouseMember.class);
        verify(houseMemberRepository).save(memberCaptor.capture());
        assertEquals(HouseRole.ADMIN, memberCaptor.getValue().getRole());
        assertEquals(creator, memberCaptor.getValue().getUser());
    }

    @Test
    void createHouseShouldFailWhenCreatorDoesNotExist() {
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Mi Piso")
                .creatorId(UUID.randomUUID())
                .build();

        when(userRepository.findById(request.getCreatorId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> houseService.createHouse(request));

        assertEquals("El usuario creador no existe", exception.getMessage());
    }

    @Test
    void joinHouseShouldAddMemberToHouse() {
        when(houseRepository.findByInviteCode(house.getInviteCode())).thenReturn(Optional.of(house));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), guest.getId())).thenReturn(Optional.empty());
        when(houseMemberRepository.save(any(HouseMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        House joinedHouse = houseService.joinHouse(house.getInviteCode(), guest.getId());

        assertEquals(house, joinedHouse);
        verify(houseMemberRepository).save(any(HouseMember.class));
    }

    @Test
    void joinHouseShouldFailWhenInviteCodeDoesNotExist() {
        when(houseRepository.findByInviteCode("BAD123")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> houseService.joinHouse("BAD123", guest.getId()));

        assertEquals("El código no existe", exception.getMessage());
    }

    @Test
    void joinHouseShouldFailWhenHouseIsDeleted() {
        house.setDeletedAt(LocalDateTime.now());
        when(houseRepository.findByInviteCode(house.getInviteCode())).thenReturn(Optional.of(house));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> houseService.joinHouse(house.getInviteCode(), guest.getId()));

        assertEquals("No puedes unirte a una casa que ha sido eliminada", exception.getMessage());
    }

    @Test
    void joinHouseShouldFailWhenUserDoesNotExist() {
        when(houseRepository.findByInviteCode(house.getInviteCode())).thenReturn(Optional.of(house));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> houseService.joinHouse(house.getInviteCode(), guest.getId()));

        assertEquals("El usuario no existe", exception.getMessage());
    }

    @Test
    void joinHouseShouldFailWhenUserIsAlreadyMember() {
        when(houseRepository.findByInviteCode(house.getInviteCode())).thenReturn(Optional.of(house));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        HouseMember existingMember = HouseMember.builder()
                .house(house)
                .user(guest)
                .active(true)
                .build();
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), guest.getId())).thenReturn(Optional.of(existingMember));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> houseService.joinHouse(house.getInviteCode(), guest.getId()));

        assertEquals("Ya eres miembro de esta vivienda", exception.getMessage());
    }

    @Test
    void getHouseDetailShouldMapHouseAndMembers() {
        HouseMember adminMember = TestFixtures.houseMember(house, creator, HouseRole.ADMIN, "#6366f1");
        HouseMember guestMember = TestFixtures.houseMember(house, guest, HouseRole.MEMBER, "#10b981");

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(adminMember));
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(adminMember, guestMember));

        HouseDetailResponse response = houseService.getHouseDetail(house.getId(), creator.getId());

        assertEquals(house.getId(), response.getId());
        assertEquals("Mi Piso", response.getName());
        assertEquals(2, response.getMembers().size());
        assertEquals(creator.getUsername(), response.getMembers().get(0).getUsername());
        assertFalse(response.getIsReadOnly());
    }

    @Test
    void getHouseDetailShouldFailWhenHouseDoesNotExist() {
        when(houseRepository.findById(house.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> houseService.getHouseDetail(house.getId(), creator.getId()));

        assertEquals("La casa no existe", exception.getMessage());
    }

    @Test
    void getHouseDetailShouldFailWhenUserIsNotParticipant() {
        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), guest.getId())).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> houseService.getHouseDetail(house.getId(), guest.getId()));
    }

    @Test
    void getUserHousesShouldReturnOnlyActiveHouses() {
        House activeHouse = TestFixtures.house("Activa", "ACT123");
        House deletedHouse = TestFixtures.house("Eliminada", "DEL123");
        deletedHouse.setDeletedAt(LocalDateTime.now());

        HouseMember activeMember = TestFixtures.houseMember(activeHouse, creator, HouseRole.ADMIN, "#6366f1");
        HouseMember deletedMember = TestFixtures.houseMember(deletedHouse, creator, HouseRole.ADMIN, "#10b981");

        when(houseMemberRepository.findByUserId(creator.getId())).thenReturn(List.of(activeMember, deletedMember));

        List<UserHouseResponse> houses = houseService.getUserHouses(creator.getId());

        assertEquals(1, houses.size());
        assertEquals(activeHouse.getName(), houses.get(0).getName());
    }

    @Test
    void getDeletedUserHousesShouldReturnOnlyDeletedHouses() {
        House activeHouse = TestFixtures.house("Activa", "ACT123");
        House deletedHouse = TestFixtures.house("Eliminada", "DEL123");
        deletedHouse.setDeletedAt(LocalDateTime.now());

        HouseMember activeMember = TestFixtures.houseMember(activeHouse, creator, HouseRole.ADMIN, "#6366f1");
        HouseMember deletedMember = TestFixtures.houseMember(deletedHouse, creator, HouseRole.ADMIN, "#10b981");

        when(houseMemberRepository.findByUserId(creator.getId())).thenReturn(List.of(activeMember, deletedMember));

        List<UserHouseResponse> houses = houseService.getDeletedUserHouses(creator.getId());

        assertEquals(1, houses.size());
        assertEquals(deletedHouse.getName(), houses.get(0).getName());
    }

    @Test
    void softDeleteHouseShouldMarkHouseAsDeleted() {
        HouseMember requester = TestFixtures.houseMember(house, creator, HouseRole.ADMIN, "#6366f1");

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(requester));
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> invocation.getArgument(0));

        houseService.softDeleteHouse(house.getId(), creator.getId());

        assertNotNull(house.getDeletedAt());
        verify(houseRepository).save(house);
    }

    @Test
    void softDeleteHouseShouldFailWhenRequesterIsNotMember() {
        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> houseService.softDeleteHouse(house.getId(), creator.getId()));

        assertEquals("No perteneces a esta casa", exception.getMessage());
    }

    @Test
    void softDeleteHouseShouldFailWhenRequesterIsNotAdmin() {
        HouseMember requester = TestFixtures.houseMember(house, creator, HouseRole.MEMBER, "#10b981");

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(requester));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> houseService.softDeleteHouse(house.getId(), creator.getId()));

        assertEquals("Solo los administradores pueden eliminar la vivienda", exception.getMessage());
    }

    @Test
    void updateHouseShouldUpdateNameAndPicture() {
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Nuevo Nombre")
                .creatorId(creator.getId())
                .profilePictureUrl("https://cdn.example.com/new-house.png")
                .build();
        HouseMember requester = TestFixtures.houseMember(house, creator, HouseRole.ADMIN, "#6366f1");

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(requester));
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(requester));

        HouseDetailResponse response = houseService.updateHouse(house.getId(), request, creator.getId());

        assertEquals("Nuevo Nombre", response.getName());
        assertEquals("https://cdn.example.com/new-house.png", response.getProfilePictureUrl());
    }

    @Test
    void updateHouseShouldFailWhenRequesterHasNoAccess() {
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Nuevo Nombre")
                .creatorId(creator.getId())
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> houseService.updateHouse(house.getId(), request, creator.getId()));

        assertEquals("Acceso denegado", exception.getMessage());
    }

    @Test
    void updateHouseShouldFailWhenHouseDoesNotExist() {
        CreateHouseRequest request = CreateHouseRequest.builder()
                .name("Nuevo Nombre")
                .creatorId(creator.getId())
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> houseService.updateHouse(house.getId(), request, creator.getId()));

        assertEquals("La casa especificada no existe", exception.getMessage());
    }

    @Test
    void removeMemberShouldDeleteMemberWhenBalanceIsZero() {
        HouseMember requester = TestFixtures.houseMember(house, creator, HouseRole.ADMIN, "#6366f1");
        HouseMember targetMember = TestFixtures.houseMember(house, guest, HouseRole.MEMBER, "#10b981");

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(requester));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), guest.getId())).thenReturn(Optional.of(targetMember));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of());

        houseService.removeMember(house.getId(), guest.getId(), creator.getId());

        assertFalse(targetMember.isActive());
        verify(houseMemberRepository).save(targetMember);
    }

    @Test
    void removeMemberShouldFailWhenBalanceIsNotSettled() {
        HouseMember requester = TestFixtures.houseMember(house, creator, HouseRole.ADMIN, "#6366f1");
        HouseMember targetMember = TestFixtures.houseMember(house, guest, HouseRole.MEMBER, "#10b981");
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Gasto")
                .amount(new BigDecimal("10.00"))
                .house(house)
                .paidBy(creator)
                .participants(List.of(guest))
                .settled(false)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(requester));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expense));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> houseService.removeMember(house.getId(), guest.getId(), creator.getId()));

        assertTrue(exception.getMessage().contains("No se puede expulsar"));
    }

    @Test
    void removeMemberShouldSucceedWhenBalanceIsWithinTolerance() {
        HouseMember requester = TestFixtures.houseMember(house, creator, HouseRole.ADMIN, "#6366f1");
        HouseMember targetMember = TestFixtures.houseMember(house, guest, HouseRole.MEMBER, "#10b981");
        // 0.04€ de deuda (por debajo del límite de 0.05€)
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Gasto")
                .amount(new BigDecimal("0.04"))
                .house(house)
                .paidBy(creator)
                .participants(List.of(guest))
                .settled(false)
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(requester));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), guest.getId())).thenReturn(Optional.of(targetMember));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expense));

        houseService.removeMember(house.getId(), guest.getId(), creator.getId());

        assertFalse(targetMember.isActive());
        verify(houseMemberRepository).save(targetMember);
    }

    @Test
    void removeMemberShouldFailWhenRequesterIsNotAdmin() {
        HouseMember requester = TestFixtures.houseMember(house, creator, HouseRole.MEMBER, "#6366f1");

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), creator.getId())).thenReturn(Optional.of(requester));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> houseService.removeMember(house.getId(), guest.getId(), creator.getId()));

        assertEquals("Acceso denegado: Solo los administradores pueden expulsar personas", exception.getMessage());
    }
}