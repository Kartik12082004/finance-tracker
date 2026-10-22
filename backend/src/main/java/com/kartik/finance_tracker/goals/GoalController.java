package com.kartik.finance_tracker.goals;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.goals.dto.CreateGoalRequest;
import com.kartik.finance_tracker.goals.dto.GoalContributionRequest;
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
        UUID userId = currentUserService.getCurrentUserId();

        GoalResponse response = goalService.createGoal(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getGoals() {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(goalService.getGoals(userId));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<GoalResponse>> getArchivedGoals() {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(goalService.getArchivedGoals(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.updateGoal(userId, id, request)
        );
    }

    @PostMapping("/{id}/contributions")
    public ResponseEntity<GoalResponse> contribute(
            @PathVariable UUID id,
            @Valid @RequestBody GoalContributionRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.contribute(
                        userId,
                        id,
                        request.sourceAccountId(),
                        request.amount()
                )
        );
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<GoalResponse> pauseGoal(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(goalService.pauseGoal(userId, id));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<GoalResponse> resumeGoal(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(goalService.resumeGoal(userId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        goalService.deleteGoal(userId, id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<GoalResponse> archiveGoal(
                @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.archiveGoal(userId, id)
        );
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<GoalResponse> unarchiveGoal(
                @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                goalService.unarchiveGoal(userId, id)
        );
    }

}
