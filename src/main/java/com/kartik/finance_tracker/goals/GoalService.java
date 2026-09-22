package com.kartik.finance_tracker.goals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.goals.dto.CreateGoalRequest;
import com.kartik.finance_tracker.goals.dto.GoalResponse;
import com.kartik.finance_tracker.goals.dto.UpdateGoalRequest;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(
            GoalRepository goalRepository,
            UserRepository userRepository
    ) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GoalResponse createGoal(
            UUID userId,
            CreateGoalRequest request
    ) {
        User user = getUser(userId);

        Goal goal = new Goal(
                user,
                request.name(),
                request.targetAmount(),
                request.targetDate()
        );

        return toResponse(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(UUID userId) {
        getUser(userId);

        return goalRepository.findByUser_Id(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GoalResponse updateGoal(
            UUID userId,
            UUID goalId,
            UpdateGoalRequest request
    ) {
        Goal goal = getGoal(userId, goalId);

        if (goal.getStatus() == GoalStatus.COMPLETED
                || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Completed or cancelled goals cannot be updated"
            );
        }

        if (request.targetAmount().compareTo(goal.getCurrentAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Target amount cannot be less than current amount"
            );
        }

        goal.update(
                request.name(),
                request.targetAmount(),
                request.targetDate()
        );

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse contribute(
            UUID userId,
            UUID goalId,
            BigDecimal contribution
    ) {
        Goal goal = getGoal(userId, goalId);

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only active goals can receive contributions"
            );
        }

        if (contribution == null || contribution.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Contribution must be greater than zero"
            );
        }

        /*
         * Contributions must not silently exceed the goal target.
         */
        if (goal.getCurrentAmount().add(contribution)
                .compareTo(goal.getTargetAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Contribution would exceed the goal target"
            );
        }

        goal.addContribution(contribution);

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse pauseGoal(
            UUID userId,
            UUID goalId
    ) {
        Goal goal = getGoal(userId, goalId);

        goal.pause();

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse resumeGoal(
            UUID userId,
            UUID goalId
    ) {
        Goal goal = getGoal(userId, goalId);

        goal.resume();

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse cancelGoal(
            UUID userId,
            UUID goalId
    ) {
        Goal goal = getGoal(userId, goalId);

        goal.cancel();

        return toResponse(goalRepository.save(goal));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private Goal getGoal(
            UUID userId,
            UUID goalId
    ) {
        return goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Goal not found"));
    }

    private GoalResponse toResponse(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
