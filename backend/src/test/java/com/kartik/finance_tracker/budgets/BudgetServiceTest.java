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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartik.finance_tracker.budgets.dto.BudgetResponse;
import com.kartik.finance_tracker.budgets.dto.CreateBudgetRequest;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
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
                categoryRepository
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

    private CreateBudgetRequest createMonthlyRequest() {
        return new CreateBudgetRequest(
                categoryId,
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );
    }
}
