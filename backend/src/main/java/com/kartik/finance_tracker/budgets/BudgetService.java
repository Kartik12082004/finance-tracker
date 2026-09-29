package com.kartik.finance_tracker.budgets;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.budgets.dto.BudgetResponse;
import com.kartik.finance_tracker.budgets.dto.CreateBudgetRequest;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(
            BudgetRepository budgetRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository
    ) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public BudgetResponse createBudget(
            UUID userId,
            CreateBudgetRequest request
    ) {
        /*
         * The authenticated user is the owner of the budget.
         * We never accept a user ID from the request body.
         */
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        /*
         * A user must never be able to create a budget against
         * another user's category.
         */
        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Category does not belong to the authenticated user"
            );
        }

        validateDateRange(request.startDate(), request.endDate());

        /*
         * Prevent duplicate budgets for the same category and period.
         * The database unique constraint provides the final guarantee.
         */
        if (budgetRepository.existsByUser_IdAndCategory_IdAndPeriodAndStartDateAndEndDate(
                userId,
                request.categoryId(),
                request.period(),
                request.startDate(),
                request.endDate()
        )) {
            throw new IllegalArgumentException(
                    "A budget already exists for this category and period"
            );
        }

        Budget budget = new Budget(
                user,
                category,
                request.amount(),
                request.period(),
                request.startDate(),
                request.endDate()
        );

        Budget savedBudget = budgetRepository.save(budget);

        return toResponse(savedBudget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return budgetRepository.findByUser_Id(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForDate(
            UUID userId,
            LocalDate date
    ) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return budgetRepository
                .findByUser_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId,
                        date,
                        date
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date"
            );
        }
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmount(),
                budget.getPeriod(),
                budget.getStartDate(),
                budget.getEndDate(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
