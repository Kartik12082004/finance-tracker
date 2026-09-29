package com.kartik.finance_tracker.goals;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kartik.finance_tracker.goals.dto.CreateGoalRequest;
import com.kartik.finance_tracker.goals.dto.GoalResponse;
import com.kartik.finance_tracker.goals.dto.UpdateGoalRequest;
import com.kartik.finance_tracker.security.CurrentUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalControllerTest {

    private GoalService goalService;
    private CurrentUserService currentUserService;
    private GoalController goalController;

    private UUID userId;
    private UUID goalId;

    private GoalResponse goalResponse;

    @BeforeEach
    void setUp() {
        goalService = mock(GoalService.class);
        currentUserService = mock(CurrentUserService.class);

        goalController = new GoalController(
                goalService,
                currentUserService
        );

        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();

        goalResponse = new GoalResponse(
                goalId,
                "Gaming PC",
                new BigDecimal("150000.00"),
                BigDecimal.ZERO,
                LocalDate.of(2027, 12, 31),
                GoalStatus.ACTIVE,
                null,
                null
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);
    }

    @Test
    void shouldCreateGoal() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Gaming PC",
                new BigDecimal("150000.00"),
                LocalDate.of(2027, 12, 31)
        );

        when(goalService.createGoal(userId, request))
                .thenReturn(goalResponse);

        ResponseEntity<GoalResponse> response =
                goalController.createGoal(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(goalResponse, response.getBody());

        verify(goalService).createGoal(userId, request);
    }

    @Test
    void shouldGetGoals() {
        when(goalService.getGoals(userId))
                .thenReturn(List.of(goalResponse));

        ResponseEntity<List<GoalResponse>> response =
                goalController.getGoals();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(goalResponse, response.getBody().get(0));

        verify(goalService).getGoals(userId);
    }

    @Test
    void shouldUpdateGoal() {
        UpdateGoalRequest request = new UpdateGoalRequest(
                "New Gaming PC",
                new BigDecimal("180000.00"),
                LocalDate.of(2028, 12, 31)
        );

        when(goalService.updateGoal(userId, goalId, request))
                .thenReturn(goalResponse);

        ResponseEntity<GoalResponse> response =
                goalController.updateGoal(goalId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(goalResponse, response.getBody());

        verify(goalService).updateGoal(
                userId,
                goalId,
                request
        );
    }

    @Test
    void shouldContributeToGoal() {
        BigDecimal amount = new BigDecimal("50000.00");

        when(goalService.contribute(userId, goalId, amount))
                .thenReturn(goalResponse);

        ResponseEntity<GoalResponse> response =
                goalController.contribute(goalId, amount);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(goalResponse, response.getBody());

        verify(goalService).contribute(
                userId,
                goalId,
                amount
        );
    }

    @Test
    void shouldPauseGoal() {
        when(goalService.pauseGoal(userId, goalId))
                .thenReturn(goalResponse);

        ResponseEntity<GoalResponse> response =
                goalController.pauseGoal(goalId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(goalResponse, response.getBody());

        verify(goalService).pauseGoal(userId, goalId);
    }

    @Test
    void shouldResumeGoal() {
        when(goalService.resumeGoal(userId, goalId))
                .thenReturn(goalResponse);

        ResponseEntity<GoalResponse> response =
                goalController.resumeGoal(goalId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(goalResponse, response.getBody());

        verify(goalService).resumeGoal(userId, goalId);
    }

    @Test
    void shouldCancelGoal() {
        when(goalService.cancelGoal(userId, goalId))
                .thenReturn(goalResponse);

        ResponseEntity<GoalResponse> response =
                goalController.cancelGoal(goalId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(goalResponse, response.getBody());

        verify(goalService).cancelGoal(userId, goalId);
    }
}
