package com.kartik.finance_tracker.goals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.goals.dto.CreateGoalRequest;
import com.kartik.finance_tracker.goals.dto.GoalResponse;
import com.kartik.finance_tracker.goals.dto.UpdateGoalRequest;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

class GoalServiceTest {

    private GoalRepository goalRepository;
    private UserRepository userRepository;
    private GoalService goalService;

    private UUID userId;
    private UUID goalId;

    private User user;
    private Goal goal;

    @BeforeEach
    void setUp() {
        goalRepository = mock(GoalRepository.class);
        userRepository = mock(UserRepository.class);

        goalService = new GoalService(
                goalRepository,
                userRepository
        );

        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();

        user = mock(User.class);

        when(user.getId()).thenReturn(userId);

        goal = new Goal(
                user,
                "Gaming PC",
                new BigDecimal("150000.00"),
                LocalDate.of(2027, 12, 31)
        );

        /*
         * Most service tests operate on an existing goal.
         * Tests that specifically need a missing goal can override this stub.
         */
        when(goalRepository.findByIdAndUser_Id(goalId, userId))
                .thenReturn(java.util.Optional.of(goal));
    }

    @Test
    void shouldCreateGoal() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Gaming PC",
                new BigDecimal("150000.00"),
                LocalDate.of(2027, 12, 31)
        );

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        when(goalRepository.save(any(Goal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.createGoal(userId, request);

        assertEquals("Gaming PC", response.name());
        assertEquals(
                new BigDecimal("150000.00"),
                response.targetAmount()
        );
        assertEquals(BigDecimal.ZERO, response.currentAmount());
        assertEquals(GoalStatus.ACTIVE, response.status());
    }

    @Test
    void shouldRejectMissingUser() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Gaming PC",
                new BigDecimal("150000.00"),
                LocalDate.of(2027, 12, 31)
        );

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> goalService.createGoal(userId, request)
        );
    }

    @Test
    void shouldGetGoalsForUser() {
        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        when(goalRepository.findByUser_Id(userId))
                .thenReturn(List.of(goal));

        List<GoalResponse> response = goalService.getGoals(userId);

        assertEquals(1, response.size());
        assertEquals("Gaming PC", response.get(0).name());
    }

    @Test
    void shouldUpdateGoal() {
        when(goalRepository.save(any(Goal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateGoalRequest request = new UpdateGoalRequest(
                "New Gaming PC",
                new BigDecimal("180000.00"),
                LocalDate.of(2028, 12, 31)
        );

        GoalResponse response = goalService.updateGoal(
                userId,
                goalId,
                request
        );

        assertEquals("New Gaming PC", response.name());
        assertEquals(
                new BigDecimal("180000.00"),
                response.targetAmount()
        );
        assertEquals(
                LocalDate.of(2028, 12, 31),
                response.targetDate()
        );
    }

    @Test
    void shouldRejectTargetBelowCurrentAmount() {
        goal.addContribution(new BigDecimal("60000.00"));

        UpdateGoalRequest request = new UpdateGoalRequest(
                "Gaming PC",
                new BigDecimal("50000.00"),
                LocalDate.of(2027, 12, 31)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.updateGoal(
                        userId,
                        goalId,
                        request
                )
        );
    }

    @Test
    void shouldAddContribution() {
        when(goalRepository.save(any(Goal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.contribute(
                userId,
                goalId,
                new BigDecimal("60000.00")
        );

        assertEquals(
                new BigDecimal("60000.00"),
                response.currentAmount()
        );
        assertEquals(GoalStatus.ACTIVE, response.status());
    }

    @Test
    void shouldCompleteGoalWhenContributionReachesTarget() {
        when(goalRepository.save(any(Goal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.contribute(
                userId,
                goalId,
                new BigDecimal("150000.00")
        );

        assertEquals(
                new BigDecimal("150000.00"),
                response.currentAmount()
        );
        assertEquals(GoalStatus.COMPLETED, response.status());
    }

    @Test
    void shouldRejectContributionExceedingTarget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        new BigDecimal("150001.00")
                )
        );
    }

    @Test
    void shouldPauseAndResumeGoal() {
        when(goalRepository.save(any(Goal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse paused = goalService.pauseGoal(userId, goalId);

        assertEquals(GoalStatus.PAUSED, paused.status());

        GoalResponse resumed = goalService.resumeGoal(userId, goalId);

        assertEquals(GoalStatus.ACTIVE, resumed.status());
    }

    @Test
    void shouldCancelGoal() {
        when(goalRepository.save(any(Goal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.cancelGoal(userId, goalId);

        assertEquals(GoalStatus.CANCELLED, response.status());
    }

    @Test
    void shouldRejectContributionToPausedGoal() {
        goal.pause();

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        new BigDecimal("1000.00")
                )
        );
    }

    @Test
    void shouldRejectContributionToCompletedGoal() {
        goal.addContribution(new BigDecimal("150000.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> goalService.contribute(
                        userId,
                        goalId,
                        new BigDecimal("1000.00")
                )
        );
    }
}
