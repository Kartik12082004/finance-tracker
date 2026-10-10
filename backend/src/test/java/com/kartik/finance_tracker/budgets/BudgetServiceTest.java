package com.kartik.finance_tracker.budgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartik.finance_tracker.budgets.dto.BudgetResponse;
import com.kartik.finance_tracker.budgets.dto.CreateBudgetRequest;
import com.kartik.finance_tracker.budgets.dto.UpdateBudgetRequest;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.categories.CategoryType;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private BudgetService budgetService;

    private UUID userId;
    private UUID categoryId;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {

        budgetService = new BudgetService(
                budgetRepository,
                userRepository,
                categoryRepository,
                transactionRepository
        );

        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        user = mock(User.class);
        category = mock(Category.class);

    }

    @Test
    void shouldCreateMonthlyBudget() {

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        categoryId,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(category.getUser()).thenReturn(user);
        when(category.getId()).thenReturn(categoryId);
        when(category.getType()).thenReturn(CategoryType.EXPENSE);
        when(category.getName()).thenReturn("Food");
        when(user.getId()).thenReturn(userId);

        when(budgetRepository
                .existsByUser_IdAndCategory_IdAndYearAndMonth(
                        userId,
                        categoryId,
                        2026,
                        9
                ))
                .thenReturn(false);

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        BudgetResponse response =
                budgetService.createBudget(
                        userId,
                        2026,
                        9,
                        request
                );

        assertEquals(categoryId, response.categoryId());
        assertEquals("Food", response.categoryName());
        assertEquals(
                new BigDecimal("10000.00"),
                response.amount()
        );
        assertEquals(2026, response.year());
        assertEquals(9, response.month());

        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        categoryId,
                        new BigDecimal("10000.123")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> budgetService.createBudget(
                                userId,
                                2026,
                                9,
                                request
                        )
                );

        assertEquals(
                "Amount cannot have more than 2 decimal places",
                exception.getMessage()
        );

        verify(categoryRepository, never())
                .findById(any());

        verify(budgetRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectMissingUser() {

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        categoryId,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.createBudget(
                        userId,
                        2026,
                        9,
                        request
                )
        );

        verify(categoryRepository, never())
                .findById(any());

        verify(budgetRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectMissingCategory() {

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        categoryId,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.createBudget(
                        userId,
                        2026,
                        9,
                        request
                )
        );

        verify(budgetRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectCategoryOwnedByAnotherUser() {

        User anotherUser = mock(User.class);
        Category anotherCategory = mock(Category.class);

        UUID anotherUserId = UUID.randomUUID();

        when(anotherUser.getId())
                .thenReturn(anotherUserId);

        when(anotherCategory.getUser())
                .thenReturn(anotherUser);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(anotherCategory));

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        categoryId,
                        new BigDecimal("10000.00")
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.createBudget(
                        userId,
                        2026,
                        9,
                        request
                )
        );

        verify(budgetRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectDuplicateMonthlyBudget() {

        CreateBudgetRequest request =
                new CreateBudgetRequest(
                        categoryId,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(category.getUser()).thenReturn(user);
        when(category.getId()).thenReturn(categoryId);
        when(category.getType()).thenReturn(CategoryType.EXPENSE);
        when(user.getId()).thenReturn(userId);

        when(budgetRepository
                .existsByUser_IdAndCategory_IdAndYearAndMonth(
                        userId,
                        categoryId,
                        2026,
                        9
                ))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.createBudget(
                        userId,
                        2026,
                        9,
                        request
                )
        );

        verify(budgetRepository, never())
                .save(any());
    }

    @Test
    void shouldInheritLatestPreviousMonthForSameCategory() {

        Budget septemberBudget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("10000.00"),
                        2026,
                        9
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(budgetRepository
                .findByUser_IdAndYearAndMonth(
                        userId,
                        2026,
                        10
                ))
                .thenReturn(List.of());

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(septemberBudget));

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        List<BudgetResponse> responses =
                budgetService.getBudgetsForMonth(
                        userId,
                        2026,
                        10
                );

        assertEquals(1, responses.size());

        BudgetResponse response =
                responses.get(0);

        assertEquals(
                new BigDecimal("10000.00"),
                response.amount()
        );

        assertEquals(2026, response.year());
        assertEquals(10, response.month());

        verify(budgetRepository)
                .save(any(Budget.class));
    }

    @Test
    void shouldNotInheritBudgetAcrossCategories() {

        UUID foodId = UUID.randomUUID();

        Category food = mock(Category.class);

        when(food.getId()).thenReturn(foodId);
        when(food.getName()).thenReturn("Food");
        when(food.getChildren()).thenReturn(List.of());

        when(user.getId()).thenReturn(userId);

        Budget foodBudget =
                new Budget(
                        user,
                        food,
                        new BigDecimal("10000.00"),
                        2026,
                        9
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(budgetRepository
                .findByUser_IdAndYearAndMonth(
                        userId,
                        2026,
                        10
                ))
                .thenReturn(List.of());

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(foodBudget));

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        List<BudgetResponse> responses =
                budgetService.getBudgetsForMonth(
                        userId,
                        2026,
                        10
                );

        assertEquals(1, responses.size());
        assertEquals(foodId, responses.get(0).categoryId());

        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void shouldNotOverwriteExistingCurrentMonthBudget() {

        Budget existingBudget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("12000.00"),
                        2026,
                        10
                );

        Budget previousBudget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("10000.00"),
                        2026,
                        9
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(budgetRepository
                .findByUser_IdAndYearAndMonth(
                        userId,
                        2026,
                        10
                ))
                .thenReturn(List.of(existingBudget));

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(
                        previousBudget,
                        existingBudget
                ));

        List<BudgetResponse> responses =
                budgetService.getBudgetsForMonth(
                        userId,
                        2026,
                        10
                );

        assertEquals(1, responses.size());

        assertEquals(
                new BigDecimal("12000.00"),
                responses.get(0).amount()
        );

        verify(budgetRepository, never())
                .save(any(Budget.class));
    }

    @Test
    void shouldUseLatestPreviousSnapshot() {

        Budget septemberBudget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("10000.00"),
                        2026,
                        9
                );

        Budget augustBudget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("8000.00"),
                        2026,
                        8
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(budgetRepository
                .findByUser_IdAndYearAndMonth(
                        userId,
                        2026,
                        10
                ))
                .thenReturn(List.of());

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(
                        augustBudget,
                        septemberBudget
                ));

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        List<BudgetResponse> responses =
                budgetService.getBudgetsForMonth(
                        userId,
                        2026,
                        10
                );

        assertEquals(
                new BigDecimal("10000.00"),
                responses.get(0).amount()
        );
    }

    @Test
    void shouldUpdateOnlySelectedMonthlySnapshot() {

        Budget budget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("10000.00"),
                        2026,
                        10
                );

        UUID budgetId = UUID.randomUUID();

        UpdateBudgetRequest request =
                new UpdateBudgetRequest(
                        new BigDecimal("12000.00")
                );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.of(budget));

        when(budgetRepository.save(budget))
                .thenReturn(budget);

        BudgetResponse response =
                budgetService.updateBudget(
                        userId,
                        budgetId,
                        request
                );

        assertEquals(
                new BigDecimal("12000.00"),
                response.amount()
        );

        assertEquals(2026, response.year());
        assertEquals(10, response.month());

        verify(budgetRepository).save(budget);
    }

    @Test
    void shouldRejectUpdatingMissingBudget() {

        UUID budgetId = UUID.randomUUID();

        UpdateBudgetRequest request =
                new UpdateBudgetRequest(
                        new BigDecimal("12000.00")
                );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.updateBudget(
                        userId,
                        budgetId,
                        request
                )
        );

        verify(budgetRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectUpdateAmountWithMoreThanTwoDecimals() {

        UUID budgetId = UUID.randomUUID();

        UpdateBudgetRequest request =
                new UpdateBudgetRequest(
                        new BigDecimal("12000.123")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> budgetService.updateBudget(
                                userId,
                                budgetId,
                                request
                        )
                );

        assertEquals(
                "Amount cannot have more than 2 decimal places",
                exception.getMessage()
        );

        verify(budgetRepository, never())
                .findByIdAndUser_Id(any(), any());

        verify(budgetRepository, never())
                .save(any());
    }

    @Test
    void shouldCalculateMonthlySpending() {

        Budget budget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("10000.00"),
                        2026,
                        9
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(budgetRepository
                .findByUser_IdAndYearAndMonth(
                        userId,
                        2026,
                        9
                ))
                .thenReturn(List.of(budget));

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        when(user.getId()).thenReturn(userId);
        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");
        when(category.getChildren()).thenReturn(List.of());

        when(transactionRepository
                .findPostedExpenseAmountForBudget(
                        userId,
                        Set.of(categoryId),
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 9, 1)
                                .atStartOfDay(
                                        ZoneId.of("Asia/Kolkata")
                                )
                                .toOffsetDateTime(),
                        LocalDate.of(2026, 10, 1)
                                .atStartOfDay(
                                        ZoneId.of("Asia/Kolkata")
                                )
                                .toOffsetDateTime()
                ))
                .thenReturn(new BigDecimal("6350.00"));

        BudgetResponse response =
                budgetService.getBudgetsForMonth(
                        userId,
                        2026,
                        9
                ).get(0);

        assertEquals(
                new BigDecimal("6350.00"),
                response.spentAmount()
        );

        assertEquals(
                new BigDecimal("3650.00"),
                response.remainingAmount()
        );

        assertEquals(
                new BigDecimal("63.50"),
                response.spendingPercentage()
        );
    }

    @Test
    void shouldIncludeDescendantSpendingInParentBudget() {

        Category parent = mock(Category.class);
        Category child = mock(Category.class);

        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        when(parent.getId()).thenReturn(parentId);
        when(parent.getName()).thenReturn("Food");
        when(parent.getChildren())
                .thenReturn(List.of(child));

        when(user.getId()).thenReturn(userId);

        when(child.getId()).thenReturn(childId);
        when(child.getChildren())
                .thenReturn(List.of());

        Budget budget =
                new Budget(
                        user,
                        parent,
                        new BigDecimal("10000.00"),
                        2026,
                        9
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(budgetRepository
                .findByUser_IdAndYearAndMonth(
                        userId,
                        2026,
                        9
                ))
                .thenReturn(List.of(budget));

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        when(transactionRepository
                .findPostedExpenseAmountForBudget(
                        userId,
                        Set.of(parentId, childId),
                        TransactionType.EXPENSE,
                        LocalDate.of(2026, 9, 1)
                                .atStartOfDay(
                                        ZoneId.of("Asia/Kolkata")
                                )
                                .toOffsetDateTime(),
                        LocalDate.of(2026, 10, 1)
                                .atStartOfDay(
                                        ZoneId.of("Asia/Kolkata")
                                )
                                .toOffsetDateTime()
                ))
                .thenReturn(new BigDecimal("4000.00"));

        BudgetResponse response =
                budgetService.getBudgetsForMonth(
                        userId,
                        2026,
                        9
                ).get(0);

        assertEquals(
                new BigDecimal("4000.00"),
                response.spentAmount()
        );
    }

    @Test
    void shouldDeleteBudget() {

        UUID budgetId = UUID.randomUUID();

        Budget budget =
                new Budget(
                        user,
                        category,
                        new BigDecimal("10000.00"),
                        2026,
                        9
                );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(
                userId,
                budgetId
        );

        verify(budgetRepository)
                .delete(budget);
    }
}
