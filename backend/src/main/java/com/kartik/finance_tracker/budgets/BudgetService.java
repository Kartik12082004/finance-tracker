package com.kartik.finance_tracker.budgets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
@Transactional
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

    /**
     * Returns the budgets for a specific month.
     *
     * Missing budgets are automatically inherited from the latest
     * previous snapshot for the same category.
     */
    public List<BudgetResponse> getBudgetsForMonth(
            UUID userId,
            int year,
            int month
    ) {
        validateMonth(month);

        User user = getUser(userId);

        List<Budget> currentBudgets =
                budgetRepository.findByUser_IdAndYearAndMonth(
                        userId,
                        year,
                        month
                );

        Map<UUID, Budget> budgetsByCategory = new HashMap<>();

        for (Budget budget : currentBudgets) {
            budgetsByCategory.put(
                    budget.getCategory().getId(),
                    budget
            );
        }

        /*
         * Find the latest existing snapshot for every category.
         *
         * Because budgets are monthly snapshots, inheritance is always
         * category-specific. A Food budget can never create a Housing
         * budget, for example.
         */
        List<Budget> allUserBudgets =
                budgetRepository.findByUser_Id(userId);

        Map<UUID, Budget> latestPreviousBudgetByCategory =
                new HashMap<>();

        for (Budget budget : allUserBudgets) {

            if (!isBefore(
                    budget.getYear(),
                    budget.getMonth(),
                    year,
                    month
            )) {
                continue;
            }

            UUID budgetCategoryId =
                    budget.getCategory().getId();

            Budget existing =
                    latestPreviousBudgetByCategory.get(
                            budgetCategoryId
                    );

            if (existing == null
                    || isAfter(
                            budget.getYear(),
                            budget.getMonth(),
                            existing.getYear(),
                            existing.getMonth()
                    )) {

                latestPreviousBudgetByCategory.put(
                        budgetCategoryId,
                        budget
                );
            }
        }

        /*
         * Materialize inherited snapshots.
         */
        for (Budget previousBudget :
                latestPreviousBudgetByCategory.values()) {

            UUID categoryId =
                    previousBudget.getCategory().getId();

            if (budgetsByCategory.containsKey(categoryId)) {
                continue;
            }

            Budget inheritedBudget = new Budget(
                    user,
                    previousBudget.getCategory(),
                    previousBudget.getAmount(),
                    year,
                    month
            );

            Budget saved =
                    budgetRepository.save(inheritedBudget);

            budgetsByCategory.put(
                    categoryId,
                    saved
            );
        }

        return budgetsByCategory.values()
                .stream()
                .sorted(
                        (first, second) ->
                                first.getCategory()
                                        .getName()
                                        .compareToIgnoreCase(
                                                second.getCategory()
                                                        .getName()
                                        )
                )
                .map(this::toResponse)
                .toList();
    }

    /**
     * Creates an explicit budget snapshot for a month.
     */
    public BudgetResponse createBudget(
            UUID userId,
            int year,
            int month,
            CreateBudgetRequest request
    ) {
        validateMonth(month);
        validateAmount(request.amount());

        User user = getUser(userId);

        Category category =
                categoryRepository.findById(request.categoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                        );

        validateCategoryOwnership(category, userId);

        if (category.getType()
                != com.kartik.finance_tracker.categories.CategoryType.EXPENSE) {

            throw new IllegalArgumentException(
                    "Budgets can only be created for expense categories"
            );
        }

        if (budgetRepository
                .existsByUser_IdAndCategory_IdAndYearAndMonth(
                        userId,
                        category.getId(),
                        year,
                        month
                )) {

            throw new IllegalArgumentException(
                    "Budget already exists for this category and month"
            );
        }

        Budget budget = new Budget(
                user,
                category,
                request.amount(),
                year,
                month
        );

        return toResponse(
                budgetRepository.save(budget)
        );
    }

    /**
     * Updates only the selected month's snapshot.
     *
     * Future snapshots are not modified.
     */
    public BudgetResponse updateBudget(
            UUID userId,
            UUID budgetId,
            UpdateBudgetRequest request
    ) {
        validateAmount(request.amount());

        Budget budget =
                budgetRepository.findByIdAndUser_Id(
                        budgetId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Budget not found"
                        )
                );

        budget.updateAmount(request.amount());

        return toResponse(
                budgetRepository.save(budget)
        );
    }

    /**
     * Deletes a single monthly snapshot.
     */
    public void deleteBudget(
            UUID userId,
            UUID budgetId
    ) {
        Budget budget =
                budgetRepository.findByIdAndUser_Id(
                        budgetId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Budget not found"
                        )
                );

        budgetRepository.delete(budget);
    }

    private BudgetResponse toResponse(Budget budget) {

        BigDecimal spentAmount =
                calculateSpentAmount(budget);

        BigDecimal remainingAmount =
                budget.getAmount()
                        .subtract(spentAmount);

        BigDecimal spendingPercentage =
                calculateSpendingPercentage(
                        budget.getAmount(),
                        spentAmount
                );

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmount(),
                spentAmount,
                remainingAmount,
                spendingPercentage,
                budget.getYear(),
                budget.getMonth(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }

    private BigDecimal calculateSpentAmount(
            Budget budget
    ) {
        Set<UUID> categoryIds =
                new HashSet<>();

        collectCategoryIds(
                budget.getCategory(),
                categoryIds
        );

        OffsetDateTime start =
                LocalDate.of(
                        budget.getYear(),
                        budget.getMonth(),
                        1
                )
                .atStartOfDay(APPLICATION_ZONE)
                .toOffsetDateTime();

        OffsetDateTime end =
                LocalDate.of(
                        budget.getYear(),
                        budget.getMonth(),
                        1
                )
                .plusMonths(1)
                .atStartOfDay(APPLICATION_ZONE)
                .toOffsetDateTime();

        BigDecimal spent =
                transactionRepository
                        .findPostedExpenseAmountForBudget(
                                budget.getUser().getId(),
                                categoryIds,
                                TransactionType.EXPENSE,
                                start,
                                end
                        );

        if (spent == null) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return spent.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    /**
     * Includes the budget category itself and every descendant.
     *
     * This is what allows a parent budget such as Food to include:
     *
     * Food
     * ├── Groceries
     * ├── Restaurants
     * └── Delivery
     */
    private void collectCategoryIds(
            Category category,
            Set<UUID> categoryIds
    ) {
        categoryIds.add(category.getId());

        if (category.getChildren() == null) {
            return;
        }

        for (Category child : category.getChildren()) {
            collectCategoryIds(child, categoryIds);
        }
    }

    private BigDecimal calculateSpendingPercentage(
            BigDecimal budgetAmount,
            BigDecimal spentAmount
    ) {
        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return spentAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        budgetAmount,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private void validateCategoryOwnership(
            Category category,
            UUID userId
    ) {
        if (!category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(
                    "Category not found"
            );
        }
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Month must be between 1 and 12"
            );
        }
    }

    private boolean isBefore(
            int year,
            int month,
            int targetYear,
            int targetMonth
    ) {
        return year < targetYear
                || (year == targetYear
                    && month < targetMonth);
    }

    private boolean isAfter(
            int year,
            int month,
            int otherYear,
            int otherMonth
    ) {
        return year > otherYear
                || (year == otherYear
                    && month > otherMonth);
    }
}
