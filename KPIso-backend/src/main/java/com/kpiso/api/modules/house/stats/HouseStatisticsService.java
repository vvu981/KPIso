package com.kpiso.api.modules.house.stats;

import com.kpiso.api.modules.expense.Expense;
import com.kpiso.api.modules.expense.ExpenseRepository;
import com.kpiso.api.modules.house.HouseMember;
import com.kpiso.api.modules.house.HouseMemberRepository;
import com.kpiso.api.modules.house.stats.dto.ExpenseDto;
import com.kpiso.api.modules.house.stats.dto.MonthlyExpenseDto;
import com.kpiso.api.modules.house.stats.dto.ProductStatsDto;
import com.kpiso.api.modules.shoppinglist.ShoppingItem;
import com.kpiso.api.modules.shoppinglist.ShoppingItemRepository;
import com.kpiso.api.modules.shoppinglist.ShoppingItemStatus;
import com.kpiso.api.modules.task.Task;
import com.kpiso.api.modules.task.TaskRepository;
import com.kpiso.api.modules.task.TaskStatus;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.kpiso.api.modules.user.User;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HouseStatisticsService {

    private final ExpenseRepository expenseRepository;
    private final ShoppingItemRepository shoppingItemRepository;
    private final TaskRepository taskRepository;
    private final HouseMemberRepository houseMemberRepository;

    public HouseStatisticsService(ExpenseRepository expenseRepository,
                                  ShoppingItemRepository shoppingItemRepository,
                                  TaskRepository taskRepository,
                                  HouseMemberRepository houseMemberRepository) {
        this.expenseRepository = expenseRepository;
        this.shoppingItemRepository = shoppingItemRepository;
        this.taskRepository = taskRepository;
        this.houseMemberRepository = houseMemberRepository;
    }

    // ── Seguridad ─────────────────────────────────────────────────────────────

    private void assertMember(UUID houseId, UUID userId) {
        // Verifica que el usuario sea miembro activo de la casa
        houseMemberRepository.findByHouseIdAndUserId(houseId, userId)
                .filter(HouseMember::isActive)
                .orElseThrow(() -> new AccessDeniedException("El usuario no es miembro activo de la vivienda"));
    }

    // ── 1. Coste de vida por persona ──────────────────────────────────────────

    /**
     * Suma la parte correspondiente de cada gasto (excluye pagos directos, identificados
     * por títulos que empiezan con "Liquidación:") por usuario participante.
     * Los gastos liquidados (settled=true) también se incluyen para reflejar
     * el coste histórico real.
     */
    public Map<UUID, BigDecimal> getLivingCostPerMember(UUID houseId, UUID userId, YearMonth month) {
        assertMember(houseId, userId);

        List<Expense> allExpenses = expenseRepository.findByHouseId(houseId);
        List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);

        Map<UUID, BigDecimal> costPerMember = new HashMap<>();
        // Inicializar con 0 para todos los miembros activos
        members.stream()
                .filter(HouseMember::isActive)
                .forEach(m -> costPerMember.put(m.getUser().getId(), BigDecimal.ZERO));

        for (Expense e : allExpenses) {
            // Excluir pagos de liquidación (Bizums / pagos directos)
            if (e.getTitle() != null && e.getTitle().startsWith("Liquidación:")) continue;

            // Filtrar por mes si se especificó
            if (month != null) {
                YearMonth expenseMonth = YearMonth.from(e.getCreatedAt());
                if (!expenseMonth.equals(month)) continue;
            }

            Map<UUID, BigDecimal> splits = calculateSplits(e);
            for (Map.Entry<UUID, BigDecimal> entry : splits.entrySet()) {
                UUID participantId = entry.getKey();
                BigDecimal amount = entry.getValue();
                if (costPerMember.containsKey(participantId)) {
                    costPerMember.merge(participantId, amount, BigDecimal::add);
                }
            }
        }

        return costPerMember;
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

    // ── 2. Evolución mensual de gastos ────────────────────────────────────────

    /**
     * Agrupa todos los gastos (excluyendo liquidaciones) por mes y suma su importe.
     */
    public List<MonthlyExpenseDto> getMonthlyExpenseEvolution(UUID houseId, UUID userId) {
        assertMember(houseId, userId);

        List<Expense> allExpenses = expenseRepository.findByHouseId(houseId);

        // Agrupar por YearMonth
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        for (Expense e : allExpenses) {
            if (e.getTitle() != null && e.getTitle().startsWith("Liquidación:")) continue;
            YearMonth ym = YearMonth.from(e.getCreatedAt());
            byMonth.merge(ym, e.getAmount(), BigDecimal::add);
        }

        return byMonth.entrySet().stream()
                .map(entry -> new MonthlyExpenseDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // ── 3. Top N gastos más caros ─────────────────────────────────────────────

    /**
     * Devuelve los N gastos de mayor importe (excluye liquidaciones), opcionalmente
     * filtrados por mes.
     */
    public List<ExpenseDto> getTopExpenses(UUID houseId, UUID userId, int limit, YearMonth month) {
        assertMember(houseId, userId);

        List<Expense> allExpenses = expenseRepository.findByHouseId(houseId);

        return allExpenses.stream()
                // Excluir liquidaciones / pagos directos
                .filter(e -> e.getTitle() == null || !e.getTitle().startsWith("Liquidación:"))
                // Filtrar por mes si se especifica
                .filter(e -> {
                    if (month == null) return true;
                    return YearMonth.from(e.getCreatedAt()).equals(month);
                })
                // Ordenar descendente por importe
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(limit)
                .map(e -> new ExpenseDto(
                        e.getId(),
                        e.getTitle(),
                        e.getAmount(),
                        e.getCreatedAt().toLocalDate(),
                        e.getPaidBy().getId()))
                .collect(Collectors.toList());
    }

    // ── 4. Hábitos de compra ──────────────────────────────────────────────────

    /**
     * Top 5 productos más recurrentes y Top 5 más caros de la lista de la compra
     * (solo ítems comprados — BOUGHT).
     */
    public ProductStatsDto getProductPurchaseStats(UUID houseId, UUID userId) {
        assertMember(houseId, userId);

        List<ShoppingItem> purchased = shoppingItemRepository
                .findByHouseIdAndStatusOrderByCreatedAtDesc(houseId, ShoppingItemStatus.BOUGHT);

        // Top 5 más recurrentes: agrupar por nombre, contar
        Map<String, List<ShoppingItem>> groupedByName = purchased.stream()
                .filter(si -> si.getName() != null)
                .collect(Collectors.groupingBy(
                        si -> si.getName().toLowerCase().trim()));

        List<ProductStatsDto.ProductStatItem> topFrequent = groupedByName.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().size(), e1.getValue().size()))
                .limit(5)
                .map(e -> {
                    String name = e.getKey();
                    double avgUnitPrice = e.getValue().stream()
                            .mapToDouble(si -> {
                                double price = si.getEstimatedPrice() != null ? si.getEstimatedPrice() : 0.0;
                                int qty = (si.getQuantity() != null && si.getQuantity() > 0) ? si.getQuantity() : 1;
                                return price / qty;
                            })
                            .average()
                            .orElse(0.0);
                    avgUnitPrice = Math.round(avgUnitPrice * 100.0) / 100.0;
                    return new ProductStatsDto.ProductStatItem(name, avgUnitPrice);
                })
                .collect(Collectors.toList());

        // Top 5 más caros: ordenados por precio unitario
        List<ProductStatsDto.ProductStatItem> allItems = purchased.stream()
                .filter(si -> si.getName() != null)
                .map(si -> {
                    String name = si.getName().toLowerCase().trim();
                    double price = si.getEstimatedPrice() != null ? si.getEstimatedPrice() : 0.0;
                    int qty = (si.getQuantity() != null && si.getQuantity() > 0) ? si.getQuantity() : 1;
                    double unitPrice = Math.round((price / qty) * 100.0) / 100.0;
                    return new ProductStatsDto.ProductStatItem(name, unitPrice);
                })
                .sorted((item1, item2) -> Double.compare(item2.getUnitPrice(), item1.getUnitPrice()))
                .collect(Collectors.toList());

        List<ProductStatsDto.ProductStatItem> topExpensive = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (ProductStatsDto.ProductStatItem item : allItems) {
            if (!seenNames.contains(item.getName())) {
                seenNames.add(item.getName());
                topExpensive.add(item);
                if (topExpensive.size() == 5) break;
            }
        }

        return new ProductStatsDto(topFrequent, topExpensive);
    }

    // ── 5. Rendimiento de convivencia (KPI) ───────────────────────────────────

    /**
     * Calcula los puntos KPI por usuario para el mes indicado (o el mes actual
     * si month es null), replicando la misma lógica que ExpenseService.
     *
     * Reglas:
     * - Tarea COMPLETED a tiempo por su responsable: +points al completador.
     * - Tarea COMPLETED fuera de plazo por otro usuario: +points rescatador, -points asignado.
     * - Tarea PENDING vencida con asignado: -points al asignado.
     */
    public Map<UUID, Integer> getTaskKpiPoints(UUID houseId, UUID userId, YearMonth month) {
        assertMember(houseId, userId);

        List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);
        List<Task> allTasks = taskRepository.findByHouseIdAndDeletedAtIsNull(houseId);

        Map<UUID, Integer> pointsMap = new HashMap<>();
        members.stream()
                .filter(HouseMember::isActive)
                .forEach(m -> pointsMap.put(m.getUser().getId(), 0));

        LocalDateTime now = LocalDateTime.now();

        for (Task t : allTasks) {
            // Determinar la fecha de referencia para filtrar por mes
            LocalDateTime evaluationDate = t.getCompletedAt() != null ? t.getCompletedAt() : t.getDueDate();
            if (evaluationDate == null) continue;
            if (month != null && !YearMonth.from(evaluationDate).equals(month)) continue;

            if (t.getStatus() == TaskStatus.COMPLETED) {
                if (t.getCompletedBy() == null) continue;

                UUID rescuerId  = t.getCompletedBy().getId();
                UUID assignedId = t.getAssignedTo() != null ? t.getAssignedTo().getId() : null;

                // Otro usuario completó la tarea fuera del plazo del asignado
                boolean lateRescue = assignedId != null
                        && !assignedId.equals(rescuerId)
                        && t.getDueDate() != null
                        && t.getCompletedAt() != null
                        && t.getCompletedAt().isAfter(t.getDueDate());

                if (lateRescue) {
                    pointsMap.computeIfPresent(rescuerId,  (k, v) -> v + t.getPoints());
                    pointsMap.computeIfPresent(assignedId, (k, v) -> v - t.getPoints());
                } else {
                    pointsMap.computeIfPresent(rescuerId,  (k, v) -> v + t.getPoints());
                }

            } else if (t.getStatus() == TaskStatus.PENDING) {
                // Tarea pendiente vencida con asignado
                if (t.getDueDate() != null && t.getDueDate().isBefore(now) && t.getAssignedTo() != null) {
                    UUID assignedId = t.getAssignedTo().getId();
                    pointsMap.computeIfPresent(assignedId, (k, v) -> v - t.getPoints());
                }
            }
        }

        return pointsMap;
    }

    /**
     * Calcula los puntos de tareas asignadas (lo que debe obtener si completa todo) por usuario para el mes indicado, o histórico total.
     */
    public Map<UUID, Integer> getAssignedKpiPoints(UUID houseId, UUID userId, YearMonth month) {
        assertMember(houseId, userId);

        List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);
        List<Task> allTasks = taskRepository.findByHouseIdAndDeletedAtIsNull(houseId);

        Map<UUID, Integer> assignedMap = new HashMap<>();
        members.stream()
                .filter(HouseMember::isActive)
                .forEach(m -> assignedMap.put(m.getUser().getId(), 0));

        for (Task t : allTasks) {
            if (t.getDueDate() == null) continue;
            if (month != null && !YearMonth.from(t.getDueDate()).equals(month)) continue;

            if (t.getAssignedTo() != null) {
                UUID assignedId = t.getAssignedTo().getId();
                assignedMap.computeIfPresent(assignedId, (k, v) -> v + t.getPoints());
            }
        }

        return assignedMap;
    }

    /**
     * Calcula la evolución mensual de puntos KPI por usuario activo en la casa.
     */
    public List<Map<String, Object>> getMonthlyKpiEvolution(UUID houseId, UUID userId) {
        assertMember(houseId, userId);

        List<HouseMember> members = houseMemberRepository.findByHouseId(houseId);
        List<Task> allTasks = taskRepository.findByHouseIdAndDeletedAtIsNull(houseId);

        Map<YearMonth, Map<UUID, Integer>> pointsByMonth = new TreeMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (Task t : allTasks) {
            LocalDateTime evaluationDate = t.getCompletedAt() != null ? t.getCompletedAt() : t.getDueDate();
            if (evaluationDate == null) continue;
            YearMonth ym = YearMonth.from(evaluationDate);

            pointsByMonth.putIfAbsent(ym, new HashMap<>());
            Map<UUID, Integer> monthPointsMap = pointsByMonth.get(ym);

            // Inicializar a 0 para miembros activos
            for (HouseMember m : members) {
                if (m.isActive()) {
                    monthPointsMap.putIfAbsent(m.getUser().getId(), 0);
                }
            }

            if (t.getStatus() == TaskStatus.COMPLETED) {
                if (t.getCompletedBy() == null) continue;

                UUID rescuerId  = t.getCompletedBy().getId();
                UUID assignedId = t.getAssignedTo() != null ? t.getAssignedTo().getId() : null;

                boolean lateRescue = assignedId != null
                        && !assignedId.equals(rescuerId)
                        && t.getDueDate() != null
                        && t.getCompletedAt() != null
                        && t.getCompletedAt().isAfter(t.getDueDate());

                if (lateRescue) {
                    if (monthPointsMap.containsKey(rescuerId)) {
                        monthPointsMap.put(rescuerId, monthPointsMap.get(rescuerId) + t.getPoints());
                    }
                    if (assignedId != null && monthPointsMap.containsKey(assignedId)) {
                        monthPointsMap.put(assignedId, monthPointsMap.get(assignedId) - t.getPoints());
                    }
                } else {
                    if (monthPointsMap.containsKey(rescuerId)) {
                        monthPointsMap.put(rescuerId, monthPointsMap.get(rescuerId) + t.getPoints());
                    }
                }

            } else if (t.getStatus() == TaskStatus.PENDING) {
                if (t.getDueDate() != null && t.getDueDate().isBefore(now) && t.getAssignedTo() != null) {
                    UUID assignedId = t.getAssignedTo().getId();
                    if (monthPointsMap.containsKey(assignedId)) {
                        monthPointsMap.put(assignedId, monthPointsMap.get(assignedId) - t.getPoints());
                    }
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<YearMonth, Map<UUID, Integer>> entry : pointsByMonth.entrySet()) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", entry.getKey().toString()); // "YYYY-MM"
            for (Map.Entry<UUID, Integer> userEntry : entry.getValue().entrySet()) {
                monthData.put(userEntry.getKey().toString(), userEntry.getValue());
            }
            result.add(monthData);
        }

        return result;
    }
}
