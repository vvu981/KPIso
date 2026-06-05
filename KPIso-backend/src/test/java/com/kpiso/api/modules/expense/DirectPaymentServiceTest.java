package com.kpiso.api.modules.expense;

import com.kpiso.api.modules.activity.ActivityLogService;
import com.kpiso.api.modules.expense.dto.CreateDirectPaymentRequest;
import com.kpiso.api.modules.expense.dto.DirectPaymentResponse;
import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.house.HouseRepository;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import com.kpiso.api.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectPaymentServiceTest {

    @Mock
    private DirectPaymentRepository directPaymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HouseRepository houseRepository;

    private ActivityLogService activityLogService;

    private DirectPaymentService directPaymentService;

    private House house;
    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        house = TestFixtures.house("Piso", "INV123");
        sender = TestFixtures.user("sender", "sender@email.com");
        recipient = TestFixtures.user("recipient", "recipient@email.com");
        activityLogService = new ActivityLogService(null) {
            @Override
            public void log(String description, String actionType, House house, User user) {
            }
        };
        directPaymentService = new DirectPaymentService(
                directPaymentRepository,
                userRepository,
                houseRepository,
                activityLogService
        );
    }

    @Test
    void createDirectPaymentShouldSucceed() {
        CreateDirectPaymentRequest request = CreateDirectPaymentRequest.builder()
                .senderId(sender.getId())
                .recipientId(recipient.getId())
                .amount(new BigDecimal("15.50"))
                .houseId(house.getId())
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(directPaymentRepository.save(any(DirectPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        DirectPaymentResponse response = directPaymentService.createDirectPayment(request);

        assertNotNull(response);
        assertEquals(sender.getId(), response.getSenderId());
        assertEquals(recipient.getId(), response.getRecipientId());
        assertEquals(new BigDecimal("15.50"), response.getAmount());
        assertFalse(response.isSettled());
    }

    @Test
    void createDirectPaymentShouldFailWhenPayingSelf() {
        CreateDirectPaymentRequest request = CreateDirectPaymentRequest.builder()
                .senderId(sender.getId())
                .recipientId(sender.getId())
                .amount(new BigDecimal("15.50"))
                .houseId(house.getId())
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directPaymentService.createDirectPayment(request));

        assertEquals("No puedes realizar un pago a ti mismo", ex.getMessage());
    }

    @Test
    void createDirectPaymentShouldFailWhenHouseNotFound() {
        CreateDirectPaymentRequest request = CreateDirectPaymentRequest.builder()
                .senderId(sender.getId())
                .recipientId(recipient.getId())
                .amount(new BigDecimal("15.50"))
                .houseId(house.getId())
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directPaymentService.createDirectPayment(request));

        assertEquals("La casa no existe", ex.getMessage());
    }

    @Test
    void getHouseDirectPaymentsShouldReturnOnlyUnsettled() {
        DirectPayment dp = DirectPayment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .amount(new BigDecimal("15.50"))
                .house(house)
                .settled(false)
                .build();

        when(directPaymentRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(dp));

        List<DirectPaymentResponse> responses = directPaymentService.getHouseDirectPayments(house.getId());

        assertEquals(1, responses.size());
        assertEquals(dp.getId(), responses.get(0).getId());
    }

    @Test
    void updateDirectPaymentShouldSucceed() {
        DirectPayment dp = DirectPayment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .amount(new BigDecimal("15.50"))
                .house(house)
                .settled(false)
                .build();

        CreateDirectPaymentRequest request = CreateDirectPaymentRequest.builder()
                .senderId(sender.getId())
                .recipientId(recipient.getId())
                .amount(new BigDecimal("20.00"))
                .houseId(house.getId())
                .build();

        when(directPaymentRepository.findById(dp.getId())).thenReturn(Optional.of(dp));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender)); // requesting user
        when(directPaymentRepository.save(any(DirectPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        DirectPaymentResponse response = directPaymentService.updateDirectPayment(dp.getId(), request, sender.getId());

        assertNotNull(response);
        assertEquals(new BigDecimal("20.00"), response.getAmount());
    }

    @Test
    void updateDirectPaymentShouldFailWhenSettled() {
        DirectPayment dp = DirectPayment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .amount(new BigDecimal("15.50"))
                .house(house)
                .settled(true)
                .build();

        CreateDirectPaymentRequest request = CreateDirectPaymentRequest.builder()
                .senderId(sender.getId())
                .recipientId(recipient.getId())
                .amount(new BigDecimal("20.00"))
                .houseId(house.getId())
                .build();

        when(directPaymentRepository.findById(dp.getId())).thenReturn(Optional.of(dp));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> directPaymentService.updateDirectPayment(dp.getId(), request, sender.getId()));

        assertEquals("No se puede editar un pago liquidado", ex.getMessage());
    }

    @Test
    void updateDirectPaymentShouldFailWhenNotOwner() {
        DirectPayment dp = DirectPayment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .amount(new BigDecimal("15.50"))
                .house(house)
                .settled(false)
                .build();

        CreateDirectPaymentRequest request = CreateDirectPaymentRequest.builder()
                .senderId(sender.getId())
                .recipientId(recipient.getId())
                .amount(new BigDecimal("20.00"))
                .houseId(house.getId())
                .build();

        when(directPaymentRepository.findById(dp.getId())).thenReturn(Optional.of(dp));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient)); // requesting user is recipient, not owner

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directPaymentService.updateDirectPayment(dp.getId(), request, recipient.getId()));

        assertEquals("Acceso denegado: No eres el emisor de este pago", ex.getMessage());
    }

    @Test
    void deleteDirectPaymentShouldSucceed() {
        DirectPayment dp = DirectPayment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .amount(new BigDecimal("15.50"))
                .house(house)
                .settled(false)
                .build();

        when(directPaymentRepository.findById(dp.getId())).thenReturn(Optional.of(dp));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));

        directPaymentService.deleteDirectPayment(dp.getId(), sender.getId());

        verify(directPaymentRepository).delete(dp);
    }

    @Test
    void deleteDirectPaymentShouldFailWhenSettled() {
        DirectPayment dp = DirectPayment.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .amount(new BigDecimal("15.50"))
                .house(house)
                .settled(true)
                .build();

        when(directPaymentRepository.findById(dp.getId())).thenReturn(Optional.of(dp));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> directPaymentService.deleteDirectPayment(dp.getId(), sender.getId()));

        assertEquals("No se puede eliminar un pago liquidado", ex.getMessage());
    }
}
