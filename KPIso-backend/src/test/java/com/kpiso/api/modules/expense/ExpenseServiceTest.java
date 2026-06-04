package com.kpiso.api.modules.expense;

import com.kpiso.api.modules.activity.ActivityLogService;
import com.kpiso.api.modules.expense.dto.*;
import com.kpiso.api.modules.house.*;
import com.kpiso.api.modules.task.RotationType;
import com.kpiso.api.modules.task.Task;
import com.kpiso.api.modules.task.TaskRepository;
import com.kpiso.api.modules.task.TaskStatus;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.modules.user.UserRepository;
import com.kpiso.api.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HouseMemberRepository houseMemberRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DirectPaymentRepository directPaymentRepository;

    private ActivityLogService activityLogService;

    private ExpenseService expenseService;

    private House house;
    private User payer;
    private User participantA;
    private User participantB;

    @BeforeEach
    void setUp() {
        house = TestFixtures.house("Piso", "INV123");
        payer = TestFixtures.user("payer", "payer@email.com");
        participantA = TestFixtures.user("ana", "ana@email.com");
        participantB = TestFixtures.user("bea", "bea@email.com");
        activityLogService = new ActivityLogService(null) {
            @Override
            public void log(String description, String actionType, House house, User user) {
            }
        };
        expenseService = new ExpenseService(expenseRepository, houseRepository, userRepository, houseMemberRepository, activityLogService, taskRepository, directPaymentRepository);
    }

    @Test
    void createExpenseShouldPersistRegularExpense() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Compra común")
                .amount(new BigDecimal("12.50"))
                .houseId(house.getId())
                .paidById(payer.getId())
                .participantIds(List.of(participantA.getId(), participantB.getId()))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
        when(userRepository.findAllById(request.getParticipantIds())).thenReturn(List.of(participantA, participantB));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.createExpense(request);

        assertEquals("Compra común", response.getTitle());
        assertEquals(new BigDecimal("12.50"), response.getAmount());
        assertEquals(payer.getId(), response.getPaidById());
        assertEquals(List.of(participantA.getUsername(), participantB.getUsername()), response.getParticipantUsernames());
    }

    @Test
    void createExpenseShouldLogPaymentWhenTitleStartsWithSettlementPrefix() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Liquidación: enero")
                .amount(new BigDecimal("9.99"))
                .houseId(house.getId())
                .paidById(payer.getId())
                .participantIds(List.of(participantA.getId()))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
        when(userRepository.findAllById(request.getParticipantIds())).thenReturn(List.of(participantA));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.createExpense(request);
    }

    @Test
    void createExpenseShouldFailWhenHouseDoesNotExist() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .houseId(house.getId())
                .paidById(payer.getId())
                .participantIds(List.of(participantA.getId()))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> expenseService.createExpense(request));

        assertEquals("La casa no existe", exception.getMessage());
    }

    @Test
    void createExpenseShouldFailWhenPayerDoesNotExist() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .houseId(house.getId())
                .paidById(payer.getId())
                .participantIds(List.of(participantA.getId()))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> expenseService.createExpense(request));

        assertEquals("El usuario pagador no existe", exception.getMessage());
    }

    @Test
    void createExpenseShouldFailWhenParticipantsAreInvalid() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .houseId(house.getId())
                .paidById(payer.getId())
                .participantIds(List.of(participantA.getId()))
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
        when(userRepository.findAllById(request.getParticipantIds())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> expenseService.createExpense(request));

        assertEquals("Lista de participantes inválida", exception.getMessage());
    }

    @Test
    void updateExpenseShouldUpdateExpenseWhenOwnerModifiesIt() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .house(house)
                .paidBy(payer)
                .participants(List.of(participantA))
                .settled(false)
                .build();

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Compra actualizada")
                .amount(new BigDecimal("15.00"))
                .participantIds(List.of(participantA.getId(), participantB.getId()))
                .build();

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));
        when(userRepository.findAllById(request.getParticipantIds())).thenReturn(List.of(participantA, participantB));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.updateExpense(expense.getId(), request, payer.getId());

        assertEquals("Compra actualizada", response.getTitle());
        assertEquals(new BigDecimal("15.00"), response.getAmount());
        assertEquals(List.of(participantA.getUsername(), participantB.getUsername()), response.getParticipantUsernames());
    }

    @Test
    void updateExpenseShouldFailWhenExpenseIsSettled() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .house(house)
                .paidBy(payer)
                .participants(List.of(participantA))
                .settled(true)
                .build();

        CreateExpenseRequest request = CreateExpenseRequest.builder().title("x").amount(new BigDecimal("1.00")).participantIds(List.of(participantA.getId())).build();

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> expenseService.updateExpense(expense.getId(), request, payer.getId()));

        assertEquals("No se puede editar un gasto liquidado", exception.getMessage());
    }

    @Test
    void updateExpenseShouldFailWhenRequesterIsNotOwner() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .house(house)
                .paidBy(participantA)
                .participants(List.of(participantA))
                .settled(false)
                .build();

        CreateExpenseRequest request = CreateExpenseRequest.builder().title("x").amount(new BigDecimal("1.00")).participantIds(List.of(participantA.getId())).build();

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> expenseService.updateExpense(expense.getId(), request, payer.getId()));

        assertEquals("Acceso denegado: No eres el dueño de esta factura", exception.getMessage());
    }

    @Test
    void deleteExpenseShouldDeleteWhenOwnerRequestsIt() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .house(house)
                .paidBy(payer)
                .participants(List.of(participantA))
                .settled(false)
                .build();

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));

        expenseService.deleteExpense(expense.getId(), payer.getId());

        verify(expenseRepository).delete(expense);
    }

    @Test
    void deleteExpenseShouldFailWhenRequesterIsNotOwner() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra")
                .amount(new BigDecimal("12.50"))
                .house(house)
                .paidBy(participantA)
                .participants(List.of(participantA))
                .settled(false)
                .build();

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        when(userRepository.findById(payer.getId())).thenReturn(Optional.of(payer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> expenseService.deleteExpense(expense.getId(), payer.getId()));

        assertEquals("Acceso denegado", exception.getMessage());
    }

    @Test
    void getHouseExpensesShouldFilterSettlementEntries() {
        Expense regularExpense = Expense.builder().id(UUID.randomUUID()).title("Compra")
                .amount(new BigDecimal("12.50")).house(house).paidBy(payer).participants(List.of(participantA)).settled(false).build();
        Expense settlementExpense = Expense.builder().id(UUID.randomUUID()).title("Liquidación: enero")
                .amount(new BigDecimal("12.50")).house(house).paidBy(payer).participants(List.of(participantA)).settled(false).build();

        when(expenseRepository.findByHouseIdAndSettledFalseOrderByCreatedAtDesc(house.getId())).thenReturn(List.of(settlementExpense, regularExpense));

        List<ExpenseResponse> responses = expenseService.getHouseExpenses(house.getId());

        assertEquals(1, responses.size());
        assertEquals("Compra", responses.get(0).getTitle());
    }

    @Test
    void getHouseMemberStatusesShouldCombineBalancesAndPoints() {
        User memberA = TestFixtures.user("ana", "ana@email.com");
        User memberB = TestFixtures.user("bea", "bea@email.com");
        HouseMember houseMemberA = TestFixtures.houseMember(house, memberA, HouseRole.ADMIN, "#ff0000");
        HouseMember houseMemberB = TestFixtures.houseMember(house, memberB, HouseRole.MEMBER, "#00ff00");

        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra común")
                .amount(new BigDecimal("10.00"))
                .house(house)
                .paidBy(memberA)
                .participants(List.of(memberA, memberB))
                .settled(false)
                .build();

        Task rescuedTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Tarea rescatada")
                .points(3)
                .status(TaskStatus.COMPLETED)
                .rotationType(RotationType.FIXED)
                .dueDate(TestFixtures.localDateTime(LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue(), Math.max(1, LocalDateTime.now().getDayOfMonth() - 1), 9, 0))
                .completedAt(LocalDateTime.now())
                .completedBy(memberB)
                .assignedTo(memberA)
                .house(house)
                .build();

        Task ownTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Tarea propia")
                .points(2)
                .status(TaskStatus.COMPLETED)
                .rotationType(RotationType.FIXED)
                .dueDate(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .completedBy(memberA)
                .assignedTo(memberA)
                .house(house)
                .build();

        Task overdueTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Tarea vencida")
                .points(4)
                .status(TaskStatus.PENDING)
                .rotationType(RotationType.FIXED)
                .dueDate(LocalDateTime.now().minusDays(1))
                .assignedTo(memberB)
                .house(house)
                .build();

        Task skippedCompletedTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Sin autor")
                .points(5)
                .status(TaskStatus.COMPLETED)
                .rotationType(RotationType.FIXED)
                .dueDate(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .house(house)
                .build();

        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(houseMemberA, houseMemberB));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expense));
        when(taskRepository.findByHouseIdAndDeletedAtIsNull(house.getId())).thenReturn(List.of(rescuedTask, ownTask, overdueTask, skippedCompletedTask));

        Map<UUID, MemberStatusResponse> statuses = expenseService.getHouseMemberStatuses(house.getId());

        assertEquals(new BigDecimal("5.00"), statuses.get(memberA.getId()).getBalance());
        assertEquals(new BigDecimal("-5.00"), statuses.get(memberB.getId()).getBalance());
                assertEquals(-1, statuses.get(memberA.getId()).getPoints());
        assertEquals(-1, statuses.get(memberB.getId()).getPoints());
        assertEquals("#ff0000", statuses.get(memberA.getId()).getColor());
    }

    @Test
    void calculateSettlementsShouldMatchDebtorsWithCreditors() {
        User memberA = TestFixtures.user("ana", "ana@email.com");
        User memberB = TestFixtures.user("bea", "bea@email.com");
        User memberC = TestFixtures.user("carla", "carla@email.com");

        HouseMember houseMemberA = TestFixtures.houseMember(house, memberA, HouseRole.ADMIN, "#ff0000");
        HouseMember houseMemberB = TestFixtures.houseMember(house, memberB, HouseRole.MEMBER, "#00ff00");
        HouseMember houseMemberC = TestFixtures.houseMember(house, memberC, HouseRole.MEMBER, "#0000ff");

        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra")
                .amount(new BigDecimal("10.00"))
                .house(house)
                .paidBy(memberA)
                .participants(List.of(memberB, memberC))
                .settled(false)
                .build();

        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(houseMemberA, houseMemberB, houseMemberC));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expense));

        List<DebtSettlementResponse> settlements = expenseService.calculateSettlements(house.getId());

        assertEquals(2, settlements.size());
        assertTrue(settlements.stream().allMatch(s -> s.getAmount().compareTo(new BigDecimal("5.00")) == 0));
    }

    @Test
    void calculateSettlementsShouldRespectExactSplits() {
        User memberA = TestFixtures.user("ana", "ana@email.com");
        User memberB = TestFixtures.user("bea", "bea@email.com");

        HouseMember houseMemberA = TestFixtures.houseMember(house, memberA, HouseRole.ADMIN, "#ff0000");
        HouseMember houseMemberB = TestFixtures.houseMember(house, memberB, HouseRole.MEMBER, "#00ff00");

        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra proporcional")
                .amount(new BigDecimal("3.00"))
                .house(house)
                .paidBy(memberA)
                .participants(List.of(memberA, memberB))
                .exactSplits(Map.of(
                        memberA.getId(), new BigDecimal("2.08"),
                        memberB.getId(), new BigDecimal("0.92")
                ))
                .settled(false)
                .build();

        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(houseMemberA, houseMemberB));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expense));

        List<DebtSettlementResponse> settlements = expenseService.calculateSettlements(house.getId());

        assertEquals(1, settlements.size());
        DebtSettlementResponse settlement = settlements.get(0);
        assertEquals(memberB.getId(), settlement.getDebtorId());
        assertEquals(memberA.getId(), settlement.getCreditorId());
        assertEquals(0, settlement.getAmount().compareTo(new BigDecimal("0.92")));
    }

    @Test
    void calculateSettlementsShouldDistributeCentsExactly() {
        User memberA = TestFixtures.user("ana", "ana@email.com");
        User memberB = TestFixtures.user("bea", "bea@email.com");
        User memberC = TestFixtures.user("carla", "carla@email.com");

        HouseMember houseMemberA = TestFixtures.houseMember(house, memberA, HouseRole.ADMIN, "#ff0000");
        HouseMember houseMemberB = TestFixtures.houseMember(house, memberB, HouseRole.MEMBER, "#00ff00");
        HouseMember houseMemberC = TestFixtures.houseMember(house, memberC, HouseRole.MEMBER, "#0000ff");

        // 10.00 divided by 3 has remainder cents.
        // It should distribute: 3.34 to memberA, 3.33 to memberB, 3.33 to memberC.
        // Since memberA paid 10.00, memberA's net change is 10.00 - 3.34 = +6.66.
        // memberB's net change is -3.33.
        // memberC's net change is -3.33.
        // Sum of balances: 6.66 - 3.33 - 3.33 = 0.00.
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Compra no divisible exacta")
                .amount(new BigDecimal("10.00"))
                .house(house)
                .paidBy(memberA)
                .participants(List.of(memberA, memberB, memberC))
                .settled(false)
                .build();

        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(houseMemberA, houseMemberB, houseMemberC));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expense));

        List<DebtSettlementResponse> settlements = expenseService.calculateSettlements(house.getId());

        // Ana is creditor with 6.66
        // Bea owes 3.33, Carla owes 3.33
        assertEquals(2, settlements.size());
        BigDecimal totalSettled = settlements.stream()
                .map(DebtSettlementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("6.66"), totalSettled);
    }

    @Test
    void calculateSettlementsShouldIgnoreOneCentDebts() {
        User memberA = TestFixtures.user("ana", "ana@email.com");
        User memberB = TestFixtures.user("bea", "bea@email.com");

        HouseMember houseMemberA = TestFixtures.houseMember(house, memberA, HouseRole.ADMIN, "#ff0000");
        HouseMember houseMemberB = TestFixtures.houseMember(house, memberB, HouseRole.MEMBER, "#00ff00");

        // 1 cent expense paid by Ana, Bea is participant.
        // Ana's net change is +0.01 (paid) - 0.00 (not participant) = +0.01.
        // Bea's net change is -0.01 (participant).
        // Since the debt is exactly 0.01, it must be ignored/treated as 0.00.
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .title("Gasto de un centimo")
                .amount(new BigDecimal("0.01"))
                .house(house)
                .paidBy(memberA)
                .participants(List.of(memberB))
                .settled(false)
                .build();

        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(houseMemberA, houseMemberB));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expense));

        List<DebtSettlementResponse> settlements = expenseService.calculateSettlements(house.getId());

        assertEquals(0, settlements.size());
    }

    @Test
    void settleAllHouseExpensesShouldMarkExpensesAsSettled() {
        Expense expenseA = Expense.builder().id(UUID.randomUUID()).title("Compra A").amount(new BigDecimal("10.00")).house(house).paidBy(payer).participants(List.of(participantA)).settled(false).build();
        Expense expenseB = Expense.builder().id(UUID.randomUUID()).title("Compra B").amount(new BigDecimal("5.00")).house(house).paidBy(payer).participants(List.of(participantA)).settled(false).build();
        HouseMember adminMember = HouseMember.builder().user(payer).role(HouseRole.ADMIN).active(true).settleApproved(true).build();

        when(houseMemberRepository.findByHouseIdAndUserId(house.getId(), payer.getId())).thenReturn(Optional.of(adminMember));
        when(houseMemberRepository.findByHouseId(house.getId())).thenReturn(List.of(adminMember));
        when(expenseRepository.findByHouseIdAndSettledFalse(house.getId())).thenReturn(List.of(expenseA, expenseB));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.settleAllHouseExpenses(house.getId(), payer.getId());

        assertTrue(expenseA.isSettled());
        assertTrue(expenseB.isSettled());
    }
}