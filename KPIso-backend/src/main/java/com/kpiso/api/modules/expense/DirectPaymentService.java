package com.kpiso.api.modules.expense;

import com.kpiso.api.modules.expense.dto.CreateDirectPaymentRequest;
import com.kpiso.api.modules.expense.dto.DirectPaymentResponse;
import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.house.HouseRepository;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import com.kpiso.api.modules.activity.ActivityLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DirectPaymentService {

    private final DirectPaymentRepository directPaymentRepository;
    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final ActivityLogService activityLogService;

    public DirectPaymentService(DirectPaymentRepository directPaymentRepository,
                                UserRepository userRepository,
                                HouseRepository houseRepository,
                                ActivityLogService activityLogService) {
        this.directPaymentRepository = directPaymentRepository;
        this.userRepository = userRepository;
        this.houseRepository = houseRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public DirectPaymentResponse createDirectPayment(CreateDirectPaymentRequest request) {
        House house = houseRepository.findById(request.getHouseId())
                .orElseThrow(() -> new IllegalArgumentException("La casa no existe"));

        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("El usuario emisor no existe"));

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new IllegalArgumentException("El usuario receptor no existe"));

        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("No puedes realizar un pago a ti mismo");
        }

        DirectPayment payment = DirectPayment.builder()
                .sender(sender)
                .recipient(recipient)
                .amount(request.getAmount())
                .house(house)
                .settled(false)
                .build();

        DirectPayment saved = directPaymentRepository.save(payment);

        String msg = String.format("%s registró un pago directo (Bizum) de %s€ a %s",
                sender.getUsername(), saved.getAmount(), recipient.getUsername());
        activityLogService.log(msg, "PAYMENT", house, sender);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DirectPaymentResponse> getHouseDirectPayments(UUID houseId) {
        return directPaymentRepository.findByHouseIdAndSettledFalse(houseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DirectPaymentResponse mapToResponse(DirectPayment payment) {
        return DirectPaymentResponse.builder()
                .id(payment.getId())
                .senderId(payment.getSender().getId())
                .senderUsername(payment.getSender().getUsername())
                .recipientId(payment.getRecipient().getId())
                .recipientUsername(payment.getRecipient().getUsername())
                .amount(payment.getAmount())
                .houseId(payment.getHouse().getId())
                .settled(payment.isSettled())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
