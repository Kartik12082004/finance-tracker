package com.kartik.finance_tracker.budgets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.budgets.dto.BudgetResponse;
import com.kartik.finance_tracker.budgets.dto.CreateBudgetRequest;
import com.kartik.finance_tracker.budgets.dto.UpdateBudgetRequest;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class BudgetService {

    private static final ZoneId APPLICATION_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(
            BudgetRepository budgetRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository
    ) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BudgetResponse createBudget(
            UUID userId,
            CreateBudgetRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Category does not belong to the authenticated user"
            );
        }

        if (request.amount().scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }

        validateDateRange(
                request.startDate(),
                request.endDate()
        );

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

    @Transactional
    public BudgetResponse updateBudget(
            UUID userId,
            UUID budgetId,
            UpdateBudgetRequest request
    ) {
        Budget budget = budgetRepository
                .findByIdAndUser_Id(budgetId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Budget not found"));

        if (request.amount().scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }

        validateDateRange(
                request.startDate(),
                request.endDate()
        );

        if (budgetRepository
                .existsByUser_IdAndCategory_IdAndPeriodAndStartDateAndEndDateAndIdNot(
                        userId,
                        budget.getCategory().getId(),
                        request.period(),
                        request.startDate(),
                        request.endDate(),
                        budgetId
                )) {
            throw new IllegalArgumentException(
                    "A budget already exists for this category and period"
            );
        }

        budget.update(
                request.amount(),
                request.period(),
                request.startDate(),
                request.endDate()
        );

        Budget savedBudget = budgetRepository.save(budget);

        return toResponse(savedBudget);
    }

    @Transactional
    public void deleteBudget(
            UUID userId,
            UUID budgetId
    ) {
        Budget budget = budgetRepository
                .findByIdAndUser_Id(budgetId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Budget not found"));

        budgetRepository.delete(budget);
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

        LocalDate startDate = budget.getStartDate();
        LocalDate endDate = budget.getEndDate();

        OffsetDateTime start = startDate
                .atStartOfDay(APPLICATION_ZONE)
                .toOffsetDateTime();

        OffsetDateTime end = endDate
                .plusDays(1)
                .atStartOfDay(APPLICATION_ZONE)
                .toOffsetDateTime();

        Set<UUID> budgetCategoryIds =
                collectCategoryAndDescendants(
                        budget.getCategory().getId(),
                        budget.getUser().getId()
                );

        BigDecimal spentAmount =
                transactionRepository.findPostedExpenseAmountForBudget(
                        budget.getUser().getId(),
                        budgetCategoryIds,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }

        spentAmount = spentAmount.setScale(
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal remainingAmount = budget.getAmount()
                .subtract(spentAmount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal spendingPercentage = spentAmount
                .divide(
                        budget.getAmount(),
                        4,
                        RoundingMode.HALF_UP
                )
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmount(),
                spentAmount,
                remainingAmount,
                spendingPercentage,
                budget.getPeriod(),
                startDate,
                endDate,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }

    private Set<UUID> collectCategoryAndDescendants(
            UUID rootCategoryId,
            UUID userId
    ) {
        List<Category> userCategories =
                categoryRepository.findAllByUser_Id(userId);

        Set<UUID> categoryIds = new HashSet<>();

        collectDescendants(
                rootCategoryId,
                userCategories,
                categoryIds
        );

        return categoryIds;
    }

    private void collectDescendants(
            UUID categoryId,
            List<Category> categories,
            Set<UUID> categoryIds
    ) {
        if (!categoryIds.add(categoryId)) {
            return;
        }

        for (Category category : categories) {
            Category parent = category.getParent();

            if (parent != null
                    && categoryId.equals(parent.getId())) {

                collectDescendants(
                        category.getId(),
                        categories,
                        categoryIds
                );
            }
        }
    }
}
