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
import java.time.LocalDateTime;
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
        if (participants.isEmpty()) throw new IllegalArgumentException("Lista de participantes inválida");

        Expense expense = Expense.builder()
                .title(request.getTitle()).amount(request.getAmount()).house(house)
                .paidBy(paidBy).participants(participants)
                .exactSplits(request.getExactSplits())
                .settled(false).build();

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

        if (expense.isSettled()) throw new IllegalStateException("No se puede editar un gasto liquidado");

        User modifier = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario solicitante inválido"));

        if (!expense.getPaidBy().getId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Acceso denegado: No eres el dueño de esta factura");
        }

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setParticipants(userRepository.findAllById(request.getParticipantIds()));
        expense.setExactSplits(request.getExactSplits());

        Expense updated = expenseRepository.save(expense);
        activityLogService.log(String.format("%s actualizó el gasto '%s'", modifier.getUsername(), expense.getTitle()), "UPDATE", expense.getHouse(), modifier);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteExpense(UUID expenseId, UUID requestingUserId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("El gasto no existe"));

        if (expense.isSettled()) throw new IllegalStateException("No se puede eliminar un gasto liquidado");

        User terminator = userRepository.findById(requestingUserId).orElseThrow(() -> new IllegalArgumentException("Usuario inválido"));
        if (!expense.getPaidBy().getId().equals(requestingUserId)) throw new IllegalArgumentException("Acceso denegado");

        activityLogService.log(String.format("%s eliminó el gasto '%s'", terminator.getUsername(), expense.getTitle()), "DELETE", expense.getHouse(), terminator);
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getHouseExpenses(UUID houseId) {
        return expenseRepository.findByHouseIdAndSettledFalseOrderByCreatedAtDesc(houseId).stream()
                .filter(e -> !e.getTitle().startsWith("Liquidación:"))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<UUID, MemberStatusResponse> getHouseMemberStatuses(UUID houseId) {
        List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);
        List<Expense> expenses = expenseRepository.findByHouseIdAndSettledFalse(houseId);
        List<Task> allTasks = taskRepository.findByHouseIdAndDeletedAtIsNull(houseId);

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

            Map<UUID, BigDecimal> splits = calculateSplits(e);
            for (Map.Entry<UUID, BigDecimal> entry : splits.entrySet()) {
                UUID pId = entry.getKey();
                rawBalances.put(pId, rawBalances.getOrDefault(pId, BigDecimal.ZERO).subtract(entry.getValue()));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int currentMonthValue = now.getMonthValue();
        int currentYearValue = now.getYear();

        for (Task t : allTasks) {
            LocalDateTime evaluationDate = t.getCompletedAt() != null ? t.getCompletedAt() : t.getDueDate();
            if (evaluationDate == null || evaluationDate.getMonthValue() != currentMonthValue || evaluationDate.getYear() != currentYearValue) {
                continue;
            }

            if (t.getStatus() == TaskStatus.COMPLETED) {
                // Protección contra registros antiguos sin traza de usuario
                if (t.getCompletedBy() == null) {
                    continue;
                }

                UUID rescuerId = t.getCompletedBy().getId();
                UUID assignedId = t.getAssignedTo() != null ? t.getAssignedTo().getId() : null;

                // Caso A Corregido: Validamos que t.getDueDate() y t.getCompletedAt() no sean nulos para evitar caídas
                if (assignedId != null && !assignedId.equals(rescuerId) && t.getDueDate() != null && t.getCompletedAt() != null && t.getCompletedAt().isAfter(t.getDueDate())) {
                    if (pointsMap.containsKey(rescuerId)) {
                        pointsMap.put(rescuerId, pointsMap.get(rescuerId) + t.getPoints());
                    }
                    if (pointsMap.containsKey(assignedId)) {
                        pointsMap.put(assignedId, pointsMap.get(assignedId) - t.getPoints());
                    }
                } else {
                    if (pointsMap.containsKey(rescuerId)) {
                        pointsMap.put(rescuerId, pointsMap.get(rescuerId) + t.getPoints());
                    }
                }
            } else if (t.getStatus() == TaskStatus.PENDING) {
                if (t.getDueDate() != null && t.getDueDate().isBefore(now) && t.getAssignedTo() != null) {
                    UUID assignedId = t.getAssignedTo().getId();
                    if (pointsMap.containsKey(assignedId)) {
                        pointsMap.put(assignedId, pointsMap.get(assignedId) - t.getPoints());
                    }
                }
            }
        }

        Map<UUID, MemberStatusResponse> statuses = new HashMap<>();
        for (HouseMember m : members) {
            UUID uId = m.getUser().getId();
            BigDecimal bal = rawBalances.get(uId);
            if (bal != null && bal.abs().compareTo(BigDecimal.valueOf(0.05)) <= 0) {
                bal = BigDecimal.ZERO;
            }
            statuses.put(uId, MemberStatusResponse.builder()
                    .balance(bal)
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
            
            Map<UUID, BigDecimal> splits = calculateSplits(e);
            for (Map.Entry<UUID, BigDecimal> entry : splits.entrySet()) {
                UUID pId = entry.getKey();
                balances.put(pId, balances.getOrDefault(pId, BigDecimal.ZERO).subtract(entry.getValue()));
            }
        }

        List<Map.Entry<UUID, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<UUID, BigDecimal>> creditors = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            BigDecimal bal = entry.getValue();
            if (bal.abs().compareTo(BigDecimal.valueOf(0.01)) <= 0) {
                continue;
            }
            if (bal.compareTo(BigDecimal.ZERO) > 0) creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), bal));
            else if (bal.compareTo(BigDecimal.ZERO) < 0) debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), bal.abs()));
        }

        List<DebtSettlementResponse> settlements = new ArrayList<>();
        int dIdx = 0, cIdx = 0;
        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            Map.Entry<UUID, BigDecimal> debtor = debtors.get(dIdx);
            Map.Entry<UUID, BigDecimal> creditor = creditors.get(cIdx);
            BigDecimal minAmount = debtor.getValue().min(creditor.getValue());

            settlements.add(DebtSettlementResponse.builder().debtorId(debtor.getKey()).debtorUsername(usernames.get(debtor.getKey())).creditorId(creditor.getKey()).creditorUsername(usernames.get(creditor.getKey())).amount(minAmount).build());
            debtor.setValue(debtor.getValue().subtract(minAmount));
            creditor.setValue(creditor.getValue().subtract(minAmount));
            if (debtor.getValue().compareTo(BigDecimal.ZERO) <= 0) dIdx++;
            if (creditor.getValue().compareTo(BigDecimal.ZERO) <= 0) cIdx++;
        }
        return settlements;
    }

    private Map<UUID, BigDecimal> calculateSplits(Expense e) {
        Map<UUID, BigDecimal> splits = new HashMap<>();
        if (e.getExactSplits() != null && !e.getExactSplits().isEmpty()) {
            return e.getExactSplits();
        }

        List<User> participants = e.getParticipants();
        int numParticipants = participants.size();
        if (numParticipants == 0) {
            return splits;
        }

        long totalCents = e.getAmount().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
        long baseShareCents = totalCents / numParticipants;
        long remainderCents = totalCents % numParticipants;

        for (int i = 0; i < numParticipants; i++) {
            long cents = baseShareCents + (i < remainderCents ? 1 : 0);
            BigDecimal share = BigDecimal.valueOf(cents).movePointLeft(2);
            splits.put(participants.get(i).getId(), share);
        }

        return splits;
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
                .id(expense.getId()).title(expense.getTitle()).amount(expense.getAmount())
                .paidById(expense.getPaidBy().getId()).paidByUsername(expense.getPaidBy().getUsername())
                .participantUsernames(expense.getParticipants().stream().map(User::getUsername).collect(Collectors.toList()))
                .settled(expense.isSettled()).createdAt(expense.getCreatedAt()).build();
    }
}