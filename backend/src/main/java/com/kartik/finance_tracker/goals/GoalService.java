package com.kartik.finance_tracker.goals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountService;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.goals.dto.CreateGoalRequest;
import com.kartik.finance_tracker.goals.dto.GoalResponse;
import com.kartik.finance_tracker.goals.dto.UpdateGoalRequest;
import com.kartik.finance_tracker.transactions.TransactionService;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AccountBalanceService accountBalanceService;

    public GoalService(
            GoalRepository goalRepository,
            UserRepository userRepository,
            AccountService accountService,
            TransactionService transactionService,
            AccountBalanceService accountBalanceService
    ) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.accountBalanceService = accountBalanceService;
    }

    @Transactional
    public GoalResponse createGoal(
            UUID userId,
            CreateGoalRequest request
    ) {
        validateAmount(request.targetAmount());

        User user = getUser(userId);

        Account goalAccount = accountService.createGoalAccount(
                userId,
                request.name()
        );

        Goal goal = new Goal(
                user,
                request.name(),
                request.targetAmount(),
                request.targetDate()
        );

        goal.setAccount(goalAccount);

        Goal savedGoal = goalRepository.save(goal);
        refreshProgress(savedGoal);

        return toResponse(savedGoal);
    }

    @Transactional
    public List<GoalResponse> getGoals(UUID userId) {
        getUser(userId);

        return goalRepository.findByUser_IdAndArchivedFalse(userId)
                .stream()
                .filter(goal -> goal.getStatus() != GoalStatus.CANCELLED)
                .map(goal -> {
                        refreshProgress(goal);
                        return toResponse(goalRepository.save(goal));
                })
                .toList();
    }

    @Transactional
    public List<GoalResponse> getArchivedGoals(UUID userId) {
        getUser(userId);

        return goalRepository.findByUser_IdAndArchivedTrue(userId)
                .stream()
                .map(goal -> {
                        refreshProgress(goal);
                        return toResponse(goalRepository.save(goal));
                })
                .toList();
    }

    @Transactional
    public GoalResponse updateGoal(
            UUID userId,
            UUID goalId,
            UpdateGoalRequest request
    ) {
        validateAmount(request.targetAmount());

        Goal goal = getGoal(userId, goalId);
        refreshProgress(goal);

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

        refreshProgress(goal);

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse contribute(
            UUID userId,
            UUID goalId,
            UUID sourceAccountId,
            BigDecimal contribution
    ) {
        Goal goal = getGoal(userId, goalId);
        refreshProgress(goal);

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only active goals can receive contributions"
            );
        }

        validateAmount(contribution);

        if (contribution.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Contribution must be greater than zero"
            );
        }

        BigDecimal projectedBalance =
                goal.getCurrentAmount().add(contribution);

        if (projectedBalance.compareTo(goal.getTargetAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Contribution would exceed the goal target"
            );
        }

        if (sourceAccountId == null) {
            throw new IllegalArgumentException(
                    "Source account is required"
            );
        }

        transactionService.createGoalContribution(
                userId,
                sourceAccountId,
                goal.getAccount().getId(),
                contribution,
                "Contribution to goal: " + goal.getName(),
                OffsetDateTime.now()
        );

        // Recalculate from persisted transactions instead of adding the
        // requested amount to the old stored currentAmount.
        refreshProgress(goal);

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse pauseGoal(UUID userId, UUID goalId) {
        Goal goal = getGoal(userId, goalId);
        refreshProgress(goal);
        goal.pause();

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse resumeGoal(UUID userId, UUID goalId) {
        Goal goal = getGoal(userId, goalId);
        refreshProgress(goal);
        goal.resume();

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse cancelGoal(UUID userId, UUID goalId) {
        Goal goal = getGoal(userId, goalId);
        refreshProgress(goal);
        goal.cancel();

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Goal not found"));

        UUID goalAccountId = goal.getAccount().getId();

        // Delete the goal first so its non-null account_id reference is removed.
        goalRepository.delete(goal);
        goalRepository.flush();

        // Delete the account only after the goal has been deleted.
        // This still rejects accounts that have financial transactions.
        accountService.deleteGoalAccount(userId, goalAccountId);
    }

    private void refreshProgress(Goal goal) {
        BigDecimal balance = accountBalanceService.calculateBalance(
                goal.getAccount().getId()
        );

        goal.synchronizeBalance(balance);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private Goal getGoal(UUID userId, UUID goalId) {
        return goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Goal not found"));
    }

    private GoalResponse toResponse(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getAccount().getId(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.isArchived(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    @Transactional
    public GoalResponse archiveGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Goal not found"));

        goal.archive();

        Goal savedGoal = goalRepository.save(goal);
        return toResponse(savedGoal);
    }

    @Transactional
    public GoalResponse unarchiveGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUser_Id(goalId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Goal not found"));

        goal.unarchive();

        Goal savedGoal = goalRepository.save(goal);
        return toResponse(savedGoal);
    }

}
