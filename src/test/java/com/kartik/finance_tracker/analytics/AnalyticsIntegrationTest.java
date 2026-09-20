package com.kartik.finance_tracker.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kartik.finance_tracker.AbstractPostgresIntegrationTest;
import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.auth.jwt.JwtService;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.categories.CategoryType;
import com.kartik.finance_tracker.transactions.Transaction;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@SpringBootTest
@Transactional
class AnalyticsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    private User user;
    private Account account;
    private Category incomeCategory;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {

        user = new User(
                "analytics@example.com",
                "hashed-password",
                "Analytics User"
        );
        userRepository.save(user);

        account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );
        accountRepository.save(account);

        incomeCategory = new Category(
                user,
                "Salary",
                CategoryType.INCOME,
                null,
                true
        );

        expenseCategory = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        categoryRepository.save(incomeCategory);
        categoryRepository.save(expenseCategory);
    }

    @Test
    void findMonthlyIncomeAndExpenses_shouldAggregateIncomeAndExpenses() {

        OffsetDateTime start = OffsetDateTime.of(
                2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );

        OffsetDateTime end = start.plusMonths(1);

        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("75000.00"),
                incomeCategory,
                start.plusDays(5)
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("25000.00"),
                expenseCategory,
                start.plusDays(10)
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("17000.00"),
                expenseCategory,
                start.plusDays(15)
        );

        Object[] result =
                transactionRepository.findMonthlyIncomeAndExpenses(
                        user.getId(),
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        Object[] values = (Object[]) result[0];

        assertThat((BigDecimal) values[0])
                .isEqualByComparingTo(new BigDecimal("75000.00"));

        assertThat((BigDecimal) values[1])
                .isEqualByComparingTo(new BigDecimal("42000.00"));
    }

    @Test
    void findMonthlyIncomeAndExpenses_shouldIgnoreTransfers() {

        OffsetDateTime start = OffsetDateTime.of(
                2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );

        OffsetDateTime end = start.plusMonths(1);

        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                incomeCategory,
                start.plusDays(5)
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("10000.00"),
                expenseCategory,
                start.plusDays(10)
        );

        // Transfers must not be counted as either income or expense.
        saveTransaction(
                TransactionType.TRANSFER,
                new BigDecimal("20000.00"),
                null,
                start.plusDays(12)
        );

        Object[] result =
                transactionRepository.findMonthlyIncomeAndExpenses(
                        user.getId(),
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        Object[] values = (Object[]) result[0];

        assertThat((BigDecimal) values[0])
                .isEqualByComparingTo(new BigDecimal("50000.00"));

        assertThat((BigDecimal) values[1])
                .isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void findMonthlyIncomeAndExpenses_shouldRespectMonthBoundaries() {

        OffsetDateTime start = OffsetDateTime.of(
                2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );

        OffsetDateTime end = start.plusMonths(1);

        // August 31 is outside the September range.
        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("10000.00"),
                incomeCategory,
                start.minusDays(1)
        );

        // September 1 is the inclusive start of the range.
        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("20000.00"),
                incomeCategory,
                start
        );

        // September 30 is inside the range.
        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("5000.00"),
                expenseCategory,
                end.minusDays(1)
        );

        // October 1 is the exclusive end of the range.
        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("7000.00"),
                expenseCategory,
                end
        );

        Object[] result =
                transactionRepository.findMonthlyIncomeAndExpenses(
                        user.getId(),
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        Object[] values = (Object[]) result[0];

        assertThat((BigDecimal) values[0])
                .isEqualByComparingTo(new BigDecimal("20000.00"));

        assertThat((BigDecimal) values[1])
                .isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void findMonthlyIncomeAndExpenses_shouldIsolateUsers() {

        OffsetDateTime start = OffsetDateTime.of(
                2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );

        OffsetDateTime end = start.plusMonths(1);

        User otherUser = new User(
                "other-analytics@example.com",
                "hashed-password",
                "Other Analytics User"
        );
        userRepository.save(otherUser);

        Account otherAccount = new Account(
                otherUser,
                "Other Savings",
                AccountType.BANK,
                "INR"
        );
        accountRepository.save(otherAccount);

        Category otherIncomeCategory = new Category(
                otherUser,
                "Salary",
                CategoryType.INCOME,
                null,
                true
        );
        categoryRepository.save(otherIncomeCategory);

        Transaction otherTransaction = new Transaction(
                otherUser,
                otherAccount,
                null,
                otherIncomeCategory,
                TransactionType.INCOME,
                new BigDecimal("90000.00"),
                "Other user salary",
                start.plusDays(5)
        );
        transactionRepository.save(otherTransaction);

        Object[] result =
                transactionRepository.findMonthlyIncomeAndExpenses(
                        user.getId(),
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        Object[] values = (Object[]) result[0];

        assertThat((BigDecimal) values[0])
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat((BigDecimal) values[1])
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findSpendingByCategory_shouldAggregateExpensesByCategory() {

        OffsetDateTime start = OffsetDateTime.of(
                2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );

        OffsetDateTime end = start.plusMonths(1);

        Category transportCategory = new Category(
                user,
                "Transport",
                CategoryType.EXPENSE,
                null,
                true
        );
        categoryRepository.save(transportCategory);

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("3000.00"),
                expenseCategory,
                start.plusDays(3)
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("2000.00"),
                expenseCategory,
                start.plusDays(8)
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("1500.00"),
                transportCategory,
                start.plusDays(12)
        );

        // Income must not appear in spending-by-category results.
        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("75000.00"),
                incomeCategory,
                start.plusDays(15)
        );

        List<Object[]> results =
                transactionRepository.findSpendingByCategory(
                        user.getId(),
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        assertThat(results).hasSize(2);

        assertThat(results.get(0)[0])
                .isEqualTo(expenseCategory.getId());

        assertThat(results.get(0)[1])
                .isEqualTo("Food");

        assertThat((BigDecimal) results.get(0)[2])
                .isEqualByComparingTo(new BigDecimal("5000.00"));

        assertThat(results.get(1)[0])
                .isEqualTo(transportCategory.getId());

        assertThat(results.get(1)[1])
                .isEqualTo("Transport");

        assertThat((BigDecimal) results.get(1)[2])
                .isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void findIncomeVsExpenseByMonth_shouldAggregateTransactionsByMonth() {

        OffsetDateTime start = OffsetDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );

        OffsetDateTime end = start.plusYears(1);

        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                incomeCategory,
                OffsetDateTime.of(
                        2026, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("12000.00"),
                expenseCategory,
                OffsetDateTime.of(
                        2026, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("60000.00"),
                incomeCategory,
                OffsetDateTime.of(
                        2026, 2, 10, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("15000.00"),
                expenseCategory,
                OffsetDateTime.of(
                        2026, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        // Transfers are excluded from both income and expense totals.
        saveTransaction(
                TransactionType.TRANSFER,
                new BigDecimal("10000.00"),
                null,
                OffsetDateTime.of(
                        2026, 2, 20, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        List<Object[]> results =
                transactionRepository.findIncomeVsExpenseByMonth(
                        user.getId(),
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        assertThat(results).hasSize(2);

        assertThat(((Number) results.get(0)[0]).intValue())
                .isEqualTo(1);

        assertThat((BigDecimal) results.get(0)[1])
                .isEqualByComparingTo(new BigDecimal("50000.00"));

        assertThat((BigDecimal) results.get(0)[2])
                .isEqualByComparingTo(new BigDecimal("12000.00"));

        assertThat(((Number) results.get(1)[0]).intValue())
                .isEqualTo(2);

        assertThat((BigDecimal) results.get(1)[1])
                .isEqualByComparingTo(new BigDecimal("60000.00"));

        assertThat((BigDecimal) results.get(1)[2])
                .isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    void getAccountBalances_shouldReturnCalculatedBalances() throws Exception {

        Account cashAccount = new Account(
                user,
                "Cash",
                AccountType.BANK,
                "INR"
        );
        accountRepository.save(cashAccount);

        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                incomeCategory,
                OffsetDateTime.of(
                        2026, 9, 5, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("12000.00"),
                expenseCategory,
                OffsetDateTime.of(
                        2026, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        Transaction cashIncome = new Transaction(
                user,
                cashAccount,
                null,
                incomeCategory,
                TransactionType.INCOME,
                new BigDecimal("5000.00"),
                "Cash income",
                OffsetDateTime.of(
                        2026, 9, 12, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );
        transactionRepository.save(cashIncome);

        String accessToken =
                jwtService.generateAccessToken(user.getId());

        /*
         * This is a real HTTP request through Spring Security.
         * The endpoint must use the authenticated user's identity and
         * calculate balances using the existing AccountBalanceService rules.
         */
        mockMvc.perform(
                get("/api/analytics/account-balances")
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
        )
                .andExpect(status().isOk())
                .andExpect(result -> {

                    String responseBody =
                            result.getResponse().getContentAsString();

                    assertThat(responseBody)
                            .contains(account.getId().toString())
                            .contains("\"accountName\":\"HDFC Savings\"")
                            .contains("\"balance\":38000.00")
                            .contains(cashAccount.getId().toString())
                            .contains("\"accountName\":\"Cash\"")
                            .contains("\"balance\":5000.00");
                });
    }

    private Transaction saveTransaction(
            TransactionType type,
            BigDecimal amount,
            Category category,
            OffsetDateTime occurredAt
    ) {

        Transaction transaction = new Transaction(
                user,
                account,
                null,
                category,
                type,
                amount,
                "Test transaction",
                occurredAt
        );

        return transactionRepository.save(transaction);
    }

    @TestConfiguration
    static class MockMvcConfig {

        @Bean
        MockMvc mockMvc(
                WebApplicationContext webApplicationContext
        ) {
            return MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply(springSecurity())
                    .build();
        }
 
    }
}
