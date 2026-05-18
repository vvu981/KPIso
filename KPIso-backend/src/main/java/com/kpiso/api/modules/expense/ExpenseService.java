package com.kpiso.api.modules.expense;

import com.kpiso.api.modules.expense.dto.*;
import com.kpiso.api.modules.house.*;
import com.kpiso.api.modules.user.*;
import com.kpiso.api.modules.task.Task;
import com.kpiso.api.modules.task.TaskRepository;
import com.kpiso.api.modules.task.TaskStatus;
import com.kpiso.api.modules.activity.ActivityLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final HouseMemberRepository houseMemberRepository;
    private final ActivityLogService activityLogService;
    private final TaskRepository taskRepository;

    public ExpenseService(ExpenseRepository expenseRepository, HouseRepository houseRepository,
                          UserRepository userRepository, HouseMemberRepository houseMemberRepository,
                          ActivityLogService activityLogService, TaskRepository taskRepository) {
        this.expenseRepository = expenseRepository;
        this.houseRepository = houseRepository;
        this.userRepository = userRepository;
        this.houseMemberRepository = houseMemberRepository;
        this.activityLogService = activityLogService;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {
        House house = houseRepository.findById(request.getHouseId())
                .orElseThrow(() -> new IllegalArgumentException("La casa no existe"));

        User paidBy = userRepository.findById(request.getPaidById())
                .orElseThrow(() -> new IllegalArgumentException("El usuario pagador no existe"));

        List<User> participants = userRepository.findAllById(request.getParticipantIds());
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("La lista de participantes es inválida");
        }

        Expense expense = Expense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .house(house)
                .paidBy(paidBy)
                .participants(participants)
                .settled(false)
                .build();

        Expense saved = expenseRepository.save(expense);

        String type = expense.getTitle().startsWith("Liquidación:") ? "PAYMENT" : "CREATE";

        String msg = type.equals("PAYMENT")
                ? String.format("%s registró un pago de %s€ hacia %s", paidBy.getUsername(), expense.getAmount(), participants.get(0).getUsername())
                : String.format("%s añadió el gasto '%s' por %s€", paidBy.getUsername(), expense.getTitle(), expense.getAmount());

        activityLogService.log(msg, type, house, paidBy);

        return mapToResponse(saved);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID expenseId, CreateExpenseRequest request, UUID requestingUserId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("El gasto no existe"));

        if (expense.isSettled()) {
            throw new IllegalStateException("No se puede editar un gasto que ya está liquidado");
        }

        User modifier = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario solicitante inválido"));

        if (!expense.getPaidBy().getId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Acceso denegado: Solo el dueño de la factura puede editarla");
        }

        String oldTitle = expense.getTitle();
        BigDecimal oldAmount = expense.getAmount();
        List<String> oldParticipants = expense.getParticipants().stream().map(User::getUsername).collect(Collectors.toList());

        List<User> newParticipants = userRepository.findAllById(request.getParticipantIds());
        if (newParticipants.isEmpty()) throw new IllegalArgumentException("Debe haber al menos un participante");

        List<String> newParticipantsNames = newParticipants.stream().map(User::getUsername).collect(Collectors.toList());

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setParticipants(newParticipants);

        Expense updated = expenseRepository.save(expense);

        List<String> addedParticipants = new ArrayList<>(newParticipantsNames);
        addedParticipants.removeAll(oldParticipants);

        List<String> removedParticipants = new ArrayList<>(oldParticipants);
        removedParticipants.removeAll(newParticipantsNames);

        List<String> changeDetails = new ArrayList<>();
        if (!oldTitle.equals(updated.getTitle())) changeDetails.add(String.format("concepto: '%s' ➔ '%s'", oldTitle, updated.getTitle()));
        if (oldAmount.compareTo(updated.getAmount()) != 0) changeDetails.add(String.format("importe: %s€ ➔ %s€", oldAmount, updated.getAmount()));
        if (!addedParticipants.isEmpty()) changeDetails.add(String.format("añadió a %s", String.join(", ", addedParticipants)));
        if (!removedParticipants.isEmpty()) changeDetails.add(String.format("quitó a %s", String.join(", ", removedParticipants)));

        String detailedMsg = changeDetails.isEmpty()
                ? String.format("%s guardó cambios en '%s' sin modificar sus valores", modifier.getUsername(), oldTitle)
                : String.format("%s modificó '%s' (%s)", modifier.getUsername(), oldTitle, String.join(", ", changeDetails));

        activityLogService.log(detailedMsg, "UPDATE", expense.getHouse(), modifier);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteExpense(UUID expenseId, UUID requestingUserId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("El gasto no existe"));

        if (expense.isSettled()) throw new IllegalStateException("No se puede eliminar un gasto liquidado");

        User terminator = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario inválido"));

        if (!expense.getPaidBy().getId().equals(requestingUserId)) throw new IllegalArgumentException("Acceso denegado");

        String msg = String.format("%s eliminó de la pizarra el gasto '%s' que valía %s€", terminator.getUsername(), expense.getTitle(), expense.getAmount());
        activityLogService.log(msg, "DELETE", expense.getHouse(), terminator);

        expenseRepository.delete(expense);
    }

    // MODIFICADO: Filtra y remueve los registros de "Liquidación" de la lista visual de la pizarra
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getHouseExpenses(UUID houseId) {
        return expenseRepository.findByHouseIdAndSettledFalseOrderByCreatedAtDesc(houseId).stream()
                .filter(e -> !e.getTitle().startsWith("Liquidación:")) // Excluye abonos de la lista común
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<UUID, MemberStatusResponse> getHouseMemberStatuses(UUID houseId) {
        List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);
        List<Expense> expenses = expenseRepository.findByHouseIdAndSettledFalse(houseId);
        List<Task> completedTasks = taskRepository.findByHouseIdAndStatusAndDeletedAtIsNull(houseId, TaskStatus.COMPLETED);

        Map<UUID, BigDecimal> rawBalances = new HashMap<>();
        Map<UUID, String> colors = new HashMap<>();
        Map<UUID, Integer> pointsMap = new HashMap<>();

        for (HouseMember m : members) {
            UUID uId = m.getUser().getId();
            rawBalances.put(uId, BigDecimal.ZERO);
            colors.put(uId, m.getColor() != null ? m.getColor() : "#6366f1");
            pointsMap.put(uId, 0);
        }

        for (Expense e : expenses) {
            UUID payerId = e.getPaidBy().getId();
            rawBalances.put(payerId, rawBalances.getOrDefault(payerId, BigDecimal.ZERO).add(e.getAmount()));

            BigDecimal share = e.getAmount().divide(BigDecimal.valueOf(e.getParticipants().size()), 2, RoundingMode.HALF_UP);
            for (User participant : e.getParticipants()) {
                UUID pId = participant.getId();
                rawBalances.put(pId, rawBalances.getOrDefault(pId, BigDecimal.ZERO).subtract(share));
            }
        }

        for (Task t : completedTasks) {
            if (t.getAssignedTo() != null) {
                UUID rId = t.getAssignedTo().getId();
                if (pointsMap.containsKey(rId)) {
                    pointsMap.put(rId, pointsMap.get(rId) + t.getPoints());
                }
            }
        }

        Map<UUID, MemberStatusResponse> statuses = new HashMap<>();
        for (HouseMember m : members) {
            UUID uId = m.getUser().getId();
            statuses.put(uId, MemberStatusResponse.builder()
                    .balance(rawBalances.get(uId))
                    .color(colors.get(uId))
                    .points(pointsMap.get(uId))
                    .build());
        }
        return statuses;
    }

    @Transactional(readOnly = true)
    public List<DebtSettlementResponse> calculateSettlements(UUID houseId) {
        List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);
        List<Expense> expenses = expenseRepository.findByHouseIdAndSettledFalse(houseId);

        Map<UUID, BigDecimal> balances = new HashMap<>();
        Map<UUID, String> usernames = new HashMap<>();

        for (HouseMember m : members) {
            balances.put(m.getUser().getId(), BigDecimal.ZERO);
            usernames.put(m.getUser().getId(), m.getUser().getUsername());
        }

        for (Expense e : expenses) {
            UUID payerId = e.getPaidBy().getId();
            balances.put(payerId, balances.getOrDefault(payerId, BigDecimal.ZERO).add(e.getAmount()));

            BigDecimal share = e.getAmount().divide(BigDecimal.valueOf(e.getParticipants().size()), 2, RoundingMode.HALF_UP);
            for (User participant : e.getParticipants()) {
                UUID pId = participant.getId();
                balances.put(pId, balances.getOrDefault(pId, BigDecimal.ZERO).subtract(share));
            }
        }

        List<Map.Entry<UUID, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<UUID, BigDecimal>> creditors = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            } else if (entry.getValue().compareTo(BigDecimal.valueOf(-0.01)) < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().abs()));
            }
        }

        List<DebtSettlementResponse> settlements = new ArrayList<>();
        int dIdx = 0, cIdx = 0;

        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            Map.Entry<UUID, BigDecimal> debtor = debtors.get(dIdx);
            Map.Entry<UUID, BigDecimal> creditor = creditors.get(cIdx);

            BigDecimal minAmount = debtor.getValue().min(creditor.getValue());

            settlements.add(DebtSettlementResponse.builder()
                    .debtorId(debtor.getKey())
                    .debtorUsername(usernames.get(debtor.getKey()))
                    .creditorId(creditor.getKey())
                    .creditorUsername(usernames.get(creditor.getKey()))
                    .amount(minAmount)
                    .build());

            debtor.setValue(debtor.getValue().subtract(minAmount));
            creditor.setValue(creditor.getValue().subtract(minAmount));

            if (debtor.getValue().compareTo(BigDecimal.valueOf(0.01)) < 0) dIdx++;
            if (creditor.getValue().compareTo(BigDecimal.valueOf(0.01)) < 0) cIdx++;
        }
        return settlements;
    }

    @Transactional
    public void settleAllHouseExpenses(UUID houseId) {
        List<Expense> expenses = expenseRepository.findByHouseIdAndSettledFalse(houseId);
        for (Expense e : expenses) {
            e.setSettled(true);
            expenseRepository.save(e);
        }
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .paidById(expense.getPaidBy().getId())
                .paidByUsername(expense.getPaidBy().getUsername())
                .participantUsernames(expense.getParticipants().stream().map(User::getUsername).collect(Collectors.toList()))
                .settled(expense.isSettled())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}