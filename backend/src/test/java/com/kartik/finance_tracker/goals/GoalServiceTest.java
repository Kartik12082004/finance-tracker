package com.kartik.finance_tracker.goals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountService;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.goals.dto.CreateGoalRequest;
import com.kartik.finance_tracker.goals.dto.GoalResponse;
import com.kartik.finance_tracker.goals.dto.UpdateGoalRequest;
import com.kartik.finance_tracker.transactions.TransactionService;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

class GoalServiceTest {

    private GoalRepository goalRepository;
    private UserRepository userRepository;
    private AccountService accountService;
    private TransactionService transactionService;
    private AccountBalanceService accountBalanceService;
    private GoalService goalService;

    private UUID userId;
    private UUID goalId;
    private UUID sourceAccountId;

    private User user;
    private Goal goal;
    private Account goalAccount;

    private BigDecimal[] goalBalance;

    @BeforeEach
    void setUp() {
        goalRepository = mock(GoalRepository.class);
        userRepository = mock(UserRepository.class);
        accountService = mock(AccountService.class);
        transactionService = mock(TransactionService.class);
        accountBalanceService = mock(AccountBalanceService.class);

        goalService = new GoalService(
                goalRepository,
                userRepository,
                accountService,
                transactionService,
                accountBalanceService
        );

        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();
        sourceAccountId = UUID.randomUUID();

        user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                new BigDecimal("0.00")
        );

        goal = new Goal(
                user,
                "Gaming PC",
                new BigDecimal("150000.00"),
                LocalDate.of(2027, 12, 31)
        );
        goal.setAccount(goalAccount);

        goalBalance = new BigDecimal[] { BigDecimal.ZERO };

        when(accountBalanceService.calculateBalance(goalAccount.getId()))
                .thenAnswer(invocation -> goalBalance[0]);

        when(goalRepository.findByIdAndUser_Id(goalId, userId))
                .thenReturn(Optional.of(goal));

        when(goalRepository.save(any(Goal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateGoal() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Gaming PC",
                new BigDecimal("150000.00"),
                LocalDate.of(2027, 12, 31)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountService.createGoalAccount(userId, "Gaming PC"))
                .thenReturn(goalAccount);

        GoalResponse response = goalService.createGoal(userId, request);

        assertEquals("Gaming PC", response.name());
        assertEquals(goalAccount.getId(), response.accountId());
        assertEquals(new BigDecimal("150000.00"), response.targetAmount());
        assertEquals(BigDecimal.ZERO, response.currentAmount());
        assertEquals(GoalStatus.ACTIVE, response.status());

        verify(accountService).createGoalAccount(userId, "Gaming PC");
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void shouldRejectGoalWithMoreThanTwoDecimalPlacesOnCreate() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Gaming PC",
                new BigDecimal("150000.123"),
                LocalDate.of(2027, 12, 31)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.createGoal(userId, request)
        );

        verifyNoInteractions(accountService);
        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void shouldRejectMissingUser() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Gaming PC",
                new BigDecimal("150000.00"),
                LocalDate.of(2027, 12, 31)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> goalService.createGoal(userId, request)
        );

        verifyNoInteractions(accountService);
    }

    @Test
    void shouldGetGoalsForUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalRepository.findByUser_IdAndArchivedFalse(userId))
                .thenReturn(List.of(goal));

        List<GoalResponse> response = goalService.getGoals(userId);

        assertEquals(1, response.size());
        assertEquals("Gaming PC", response.get(0).name());
        assertEquals(goalAccount.getId(), response.get(0).accountId());
        assertEquals(BigDecimal.ZERO, response.get(0).currentAmount());
    }

    @Test
    void shouldNotReturnCancelledGoals() {
        Goal cancelledGoal = new Goal(
                user,
                "Cancelled Goal",
                new BigDecimal("5000.00"),
                LocalDate.of(2027, 12, 31)
        );

        cancelledGoal.setAccount(new Account(
                user,
                "Goal - Cancelled Goal",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        ));

        cancelledGoal.cancel();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalRepository.findByUser_IdAndArchivedFalse(userId))
                .thenReturn(List.of(goal, cancelledGoal));

        List<GoalResponse> response = goalService.getGoals(userId);

        assertEquals(1, response.size());
        assertEquals("Gaming PC", response.get(0).name());
    }

    @Test
    void shouldUpdateGoal() {
        UpdateGoalRequest request = new UpdateGoalRequest(
                "New Gaming PC",
                new BigDecimal("180000.00"),
                LocalDate.of(2028, 12, 31)
        );

        GoalResponse response =
                goalService.updateGoal(userId, goalId, request);

        assertEquals("New Gaming PC", response.name());
        assertEquals(goalAccount.getId(), response.accountId());
        assertEquals(new BigDecimal("180000.00"), response.targetAmount());
        assertEquals(LocalDate.of(2028, 12, 31), response.targetDate());
    }

    @Test
    void shouldRejectGoalWithMoreThanTwoDecimalPlacesOnUpdate() {
        UpdateGoalRequest request = new UpdateGoalRequest(
                "Gaming PC",
                new BigDecimal("180000.123"),
                LocalDate.of(2028, 12, 31)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.updateGoal(userId, goalId, request)
        );

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void shouldRejectTargetBelowActualGoalBalance() {
        goalBalance[0] = new BigDecimal("60000.00");

        UpdateGoalRequest request = new UpdateGoalRequest(
                "Gaming PC",
                new BigDecimal("50000.00"),
                LocalDate.of(2027, 12, 31)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.updateGoal(userId, goalId, request)
        );
    }

    @Test
    void shouldCreateTransferAndUseActualGoalAccountBalance() {
        goalBalance[0] = new BigDecimal("10000.00");

        doAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(3);
            goalBalance[0] = goalBalance[0].add(amount);
            return null;
        }).when(transactionService).createGoalContribution(
                eq(userId),
                eq(sourceAccountId),
                eq(goalAccount.getId()),
                eq(new BigDecimal("60000.00")),
                anyString(),
                any(OffsetDateTime.class)
        );

        GoalResponse response = goalService.contribute(
                userId,
                goalId,
                sourceAccountId,
                new BigDecimal("60000.00")
        );

        assertEquals(
                new BigDecimal("70000.00"),
                response.currentAmount()
        );
        assertEquals(GoalStatus.ACTIVE, response.status());

        verify(transactionService).createGoalContribution(
                eq(userId),
                eq(sourceAccountId),
                eq(goalAccount.getId()),
                eq(new BigDecimal("60000.00")),
                anyString(),
                any(OffsetDateTime.class)
        );
    }

    @Test
    void shouldCompleteGoalWhenContributionReachesTarget() {
        goalBalance[0] = new BigDecimal("100000.00");

        doAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(3);
            goalBalance[0] = goalBalance[0].add(amount);
            return null;
        }).when(transactionService).createGoalContribution(
                eq(userId),
                eq(sourceAccountId),
                eq(goalAccount.getId()),
                eq(new BigDecimal("50000.00")),
                anyString(),
                any(OffsetDateTime.class)
        );

        GoalResponse response = goalService.contribute(
                userId,
                goalId,
                sourceAccountId,
                new BigDecimal("50000.00")
        );

        assertEquals(new BigDecimal("150000.00"), response.currentAmount());
        assertEquals(GoalStatus.COMPLETED, response.status());
    }

    @Test
    void shouldRejectContributionWithMoreThanTwoDecimalPlaces() {
        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        sourceAccountId,
                        new BigDecimal("1000.123")
                )
        );

        verify(transactionService, never()).createGoalContribution(
                any(), any(), any(), any(), anyString(), any()
        );
    }

    @Test
    void shouldRejectZeroContribution() {
        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        sourceAccountId,
                        BigDecimal.ZERO
                )
        );

        verify(transactionService, never()).createGoalContribution(
                any(), any(), any(), any(), anyString(), any()
        );
    }

    @Test
    void shouldRejectContributionExceedingTarget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        sourceAccountId,
                        new BigDecimal("150001.00")
                )
        );

        verify(transactionService, never()).createGoalContribution(
                any(), any(), any(), any(), anyString(), any()
        );
    }

    @Test
    void shouldPauseAndResumeGoal() {
        GoalResponse paused = goalService.pauseGoal(userId, goalId);
        assertEquals(GoalStatus.PAUSED, paused.status());

        GoalResponse resumed = goalService.resumeGoal(userId, goalId);
        assertEquals(GoalStatus.ACTIVE, resumed.status());
    }

    @Test
    void shouldCancelGoal() {
        GoalResponse response = goalService.cancelGoal(userId, goalId);

        assertEquals(GoalStatus.CANCELLED, response.status());
    }

    @Test
    void shouldDeleteGoalAndItsGoalAccount() {
        goalService.deleteGoal(userId, goalId);

        verify(accountService).deleteGoalAccount(userId, goalAccount.getId());
        verify(goalRepository).delete(goal);
    }

    @Test
    void shouldRejectContributionToPausedGoal() {
        goal.pause();

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        sourceAccountId,
                        new BigDecimal("1000.00")
                )
        );

        verify(transactionService, never()).createGoalContribution(
                any(), any(), any(), any(), anyString(), any()
        );
    }

    @Test
    void shouldRejectContributionToCompletedGoal() {
        goalBalance[0] = new BigDecimal("150000.00");

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        sourceAccountId,
                        new BigDecimal("1000.00")
                )
        );

        verify(transactionService, never()).createGoalContribution(
                any(), any(), any(), any(), anyString(), any()
        );
    }
}
