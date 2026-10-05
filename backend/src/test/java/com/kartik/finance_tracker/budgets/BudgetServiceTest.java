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
    void shouldCreateBudget() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                categoryId,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(user.getId()).thenReturn(userId);
        when(category.getId()).thenReturn(categoryId);
        when(category.getUser()).thenReturn(user);
        when(category.getName()).thenReturn("Food");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(budgetRepository
                .existsByUser_IdAndCategory_IdAndPeriodAndStartDateAndEndDate(
                        userId,
                        categoryId,
                        BudgetPeriod.MONTHLY,
                        request.startDate(),
                        request.endDate()
                ))
                .thenReturn(false);

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response =
                budgetService.createBudget(userId, request);

        assertEquals(categoryId, response.categoryId());
        assertEquals("Food", response.categoryName());
        assertEquals(new BigDecimal("10000.00"), response.amount());
        assertEquals(BudgetPeriod.MONTHLY, response.period());
        assertEquals(request.startDate(), response.startDate());
        assertEquals(request.endDate(), response.endDate());

        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                categoryId,
                new BigDecimal("10000.123"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(category.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.createBudget(userId, request)
        );

        assertEquals(
                "Amount cannot have more than 2 decimal places",
                exception.getMessage()
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenUserDoesNotExist() {
        CreateBudgetRequest request = createMonthlyRequest();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.createBudget(userId, request)
        );

        verify(categoryRepository, never()).findById(any());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenCategoryDoesNotExist() {
        CreateBudgetRequest request = createMonthlyRequest();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.createBudget(userId, request)
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectCategoryOwnedByAnotherUser() {
        User anotherUser = mock(User.class);
        Category anotherUsersCategory = mock(Category.class);

        UUID anotherUserId = UUID.randomUUID();

        when(anotherUser.getId()).thenReturn(anotherUserId);
        when(anotherUsersCategory.getUser()).thenReturn(anotherUser);

        CreateBudgetRequest request = createMonthlyRequest();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(anotherUsersCategory));

        /*
         * A category belonging to another user must never be usable
         * for creating this user's budget.
         */
        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.createBudget(userId, request)
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidDateRange() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                categoryId,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1)
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(category.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        /*
         * A budget cannot have an end date before its start date.
         */
        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.createBudget(userId, request)
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateBudget() {
        CreateBudgetRequest request = createMonthlyRequest();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(category.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        when(budgetRepository
                .existsByUser_IdAndCategory_IdAndPeriodAndStartDateAndEndDate(
                        userId,
                        categoryId,
                        BudgetPeriod.MONTHLY,
                        request.startDate(),
                        request.endDate()
                ))
                .thenReturn(true);

        /*
         * The service rejects the duplicate before persistence.
         * The database unique constraint remains the final safeguard.
         */
        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.createBudget(userId, request)
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldGetBudgetsForUser() {
        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        List<BudgetResponse> responses =
                budgetService.getBudgets(userId);

        assertEquals(1, responses.size());
        assertEquals(categoryId, responses.get(0).categoryId());
        assertEquals("Food", responses.get(0).categoryName());
        assertEquals(
                new BigDecimal("10000.00"),
                responses.get(0).amount()
        );
    }

    @Test
    void shouldRejectGettingBudgetsForMissingUser() {
        when(userRepository.existsById(userId))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.getBudgets(userId)
        );

        verify(budgetRepository, never()).findByUser_Id(any());
    }

    @Test
    void shouldGetBudgetsForDate() {
        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        LocalDate requestedDate = LocalDate.of(2026, 9, 15);

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(budgetRepository
                .findByUser_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId,
                        requestedDate,
                        requestedDate
                ))
                .thenReturn(List.of(budget));

        List<BudgetResponse> responses =
                budgetService.getBudgetsForDate(
                        userId,
                        requestedDate
                );

        assertEquals(1, responses.size());
        assertEquals(categoryId, responses.get(0).categoryId());
        assertEquals("Food", responses.get(0).categoryName());
    }

    @Test
    void shouldCalculateBudgetSpendingPercentage() {
        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");
        when(user.getId()).thenReturn(userId);

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        when(transactionRepository.findPostedExpenseAmountForBudget(
                userId,
                Set.of(categoryId),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 9, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime(),
                LocalDate.of(2026, 10, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime()
        )).thenReturn(new BigDecimal("6350.00"));

        List<BudgetResponse> responses =
                budgetService.getBudgets(userId);

        BudgetResponse response = responses.get(0);

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
    void shouldReturnZeroSpendingForBudgetWithNoExpenses() {
        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");
        when(user.getId()).thenReturn(userId);

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        when(transactionRepository.findPostedExpenseAmountForBudget(
                userId,
                Set.of(categoryId),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 9, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime(),
                LocalDate.of(2026, 10, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime()
        )).thenReturn(BigDecimal.ZERO);

        BudgetResponse response =
                budgetService.getBudgets(userId).get(0);

        assertEquals(
                new BigDecimal("0.00"),
                response.spentAmount()
        );

        assertEquals(
                new BigDecimal("10000.00"),
                response.remainingAmount()
        );

        assertEquals(
                new BigDecimal("0.00"),
                response.spendingPercentage()
        );
    }

    @Test
    void shouldReturn100PercentWhenBudgetIsFullySpent() {
        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");
        when(user.getId()).thenReturn(userId);

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        when(transactionRepository.findPostedExpenseAmountForBudget(
                userId,
                Set.of(categoryId),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 9, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime(),
                LocalDate.of(2026, 10, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime()
        )).thenReturn(new BigDecimal("10000.00"));

        BudgetResponse response =
                budgetService.getBudgets(userId).get(0);

        assertEquals(
                new BigDecimal("10000.00"),
                response.spentAmount()
        );

        assertEquals(
                new BigDecimal("0.00"),
                response.remainingAmount()
        );

        assertEquals(
                new BigDecimal("100.00"),
                response.spendingPercentage()
        );
    }

    @Test
    void shouldAllowSpendingPercentageAbove100Percent() {
        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("100.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");
        when(user.getId()).thenReturn(userId);

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        when(transactionRepository.findPostedExpenseAmountForBudget(
                userId,
                Set.of(categoryId),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 9, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime(),
                LocalDate.of(2026, 10, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime()
        )).thenReturn(new BigDecimal("110.00"));

        BudgetResponse response =
                budgetService.getBudgets(userId).get(0);

        assertEquals(
                new BigDecimal("110.00"),
                response.spentAmount()
        );

        assertEquals(
                new BigDecimal("-10.00"),
                response.remainingAmount()
        );

        assertEquals(
                new BigDecimal("110.00"),
                response.spendingPercentage()
        );
    }

    @Test
    void shouldIncludeDescendantCategorySpendingInParentBudget() {
        UUID groceriesId = UUID.randomUUID();
        UUID restaurantsId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        Category groceries = mock(Category.class);
        Category restaurants = mock(Category.class);
        Category delivery = mock(Category.class);

        when(user.getId()).thenReturn(userId);

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");

        when(groceries.getId()).thenReturn(groceriesId);
        when(groceries.getParent()).thenReturn(category);

        when(restaurants.getId()).thenReturn(restaurantsId);
        when(restaurants.getParent()).thenReturn(category);

        when(delivery.getId()).thenReturn(deliveryId);
        when(delivery.getParent()).thenReturn(restaurants);

        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(budgetRepository.findByUser_Id(userId))
                .thenReturn(List.of(budget));

        when(categoryRepository.findAllByUser_Id(userId))
                .thenReturn(List.of(
                        category,
                        groceries,
                        restaurants,
                        delivery
                ));

        when(transactionRepository.findPostedExpenseAmountForBudget(
                userId,
                Set.of(
                        categoryId,
                        groceriesId,
                        restaurantsId,
                        deliveryId
                ),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 9, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime(),
                LocalDate.of(2026, 10, 1)
                        .atStartOfDay(ZoneId.of("Asia/Kolkata"))
                        .toOffsetDateTime()
        )).thenReturn(new BigDecimal("3500.00"));

        BudgetResponse response =
                budgetService.getBudgets(userId).get(0);

        assertEquals(
                new BigDecimal("3500.00"),
                response.spentAmount()
        );

        assertEquals(
                new BigDecimal("6500.00"),
                response.remainingAmount()
        );

        assertEquals(
                new BigDecimal("35.00"),
                response.spendingPercentage()
        );
    }

    @Test
    void shouldUpdateBudget() {
        UUID budgetId = UUID.randomUUID();

        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        setBudgetId(budget, budgetId);

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("15000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.of(budget));

        when(budgetRepository
                .existsByUser_IdAndCategory_IdAndPeriodAndStartDateAndEndDateAndIdNot(
                        userId,
                        categoryId,
                        BudgetPeriod.MONTHLY,
                        request.startDate(),
                        request.endDate(),
                        budgetId
                ))
                .thenReturn(false);

        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Food");

        when(transactionRepository.findPostedExpenseAmountForBudget(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(BigDecimal.ZERO);

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response =
                budgetService.updateBudget(
                        userId,
                        budgetId,
                        request
                );

        assertEquals(
                new BigDecimal("15000.00"),
                response.amount()
        );

        assertEquals(
                BudgetPeriod.MONTHLY,
                response.period()
        );

        assertEquals(
                request.startDate(),
                response.startDate()
        );

        assertEquals(
                request.endDate(),
                response.endDate()
        );

        verify(budgetRepository).save(budget);
    }

    @Test
    void shouldRejectUpdatingMissingBudget() {
        UUID budgetId = UUID.randomUUID();

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("15000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
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

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingBudgetOwnedByAnotherUser() {
        UUID budgetId = UUID.randomUUID();

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("15000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
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

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingAmountWithMoreThanTwoDecimalPlaces() {
        UUID budgetId = UUID.randomUUID();

        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("15000.123"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.of(budget));

        IllegalArgumentException exception = assertThrows(
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

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingInvalidDateRange() {
        UUID budgetId = UUID.randomUUID();

        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("15000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1)
        );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.of(budget));

        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.updateBudget(
                        userId,
                        budgetId,
                        request
                )
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingToDuplicateBudget() {
        UUID budgetId = UUID.randomUUID();

        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        setBudgetId(budget, budgetId);

        when(category.getId()).thenReturn(categoryId);

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("15000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.of(budget));

        when(budgetRepository
                .existsByUser_IdAndCategory_IdAndPeriodAndStartDateAndEndDateAndIdNot(
                        userId,
                        categoryId,
                        BudgetPeriod.MONTHLY,
                        request.startDate(),
                        request.endDate(),
                        budgetId
                ))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.updateBudget(
                        userId,
                        budgetId,
                        request
                )
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldDeleteBudget() {
        UUID budgetId = UUID.randomUUID();

        Budget budget = new Budget(
                user,
                category,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(userId, budgetId);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void shouldRejectDeletingMissingBudget() {
        UUID budgetId = UUID.randomUUID();

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.deleteBudget(
                        userId,
                        budgetId
                )
        );

        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void shouldRejectDeletingBudgetOwnedByAnotherUser() {
        UUID budgetId = UUID.randomUUID();

        when(budgetRepository.findByIdAndUser_Id(
                budgetId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.deleteBudget(
                        userId,
                        budgetId
                )
        );

        verify(budgetRepository, never()).delete(any());
    }

    private CreateBudgetRequest createMonthlyRequest() {
        return new CreateBudgetRequest(
                categoryId,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );
    }

    private void setBudgetId(Budget budget, UUID budgetId) {
        try {
            var idField = Budget.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(budget, budgetId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not assign test budget ID",
                    exception
            );
        }
    }
}
