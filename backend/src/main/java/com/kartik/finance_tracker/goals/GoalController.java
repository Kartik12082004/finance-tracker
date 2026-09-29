package com.kartik.finance_tracker.goals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.goals.dto.CreateGoalRequest;
import com.kartik.finance_tracker.goals.dto.GoalResponse;
import com.kartik.finance_tracker.goals.dto.UpdateGoalRequest;
import com.kartik.finance_tracker.security.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final CurrentUserService currentUserService;

    public GoalController(
            GoalService goalService,
            CurrentUserService currentUserService
    ) {
        this.goalService = goalService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @Valid @RequestBody CreateGoalRequest request
    ) {
        // The authenticated user owns the goal.
        UUID userId = currentUserService.getCurrentUserId();

        GoalResponse response = goalService.createGoal(
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getGoals() {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.getGoals(userId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.updateGoal(
                        userId,
                        id,
                        request
                )
        );
    }

    @PostMapping("/{id}/contributions")
    public ResponseEntity<GoalResponse> contribute(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.contribute(
                        userId,
                        id,
                        amount
                )
        );
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<GoalResponse> pauseGoal(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.pauseGoal(userId, id)
        );
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<GoalResponse> resumeGoal(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.resumeGoal(userId, id)
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<GoalResponse> cancelGoal(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.cancelGoal(userId, id)
        );
    }
}
