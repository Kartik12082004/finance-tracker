package com.kartik.finance_tracker.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.AbstractPostgresIntegrationTest;
import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
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
class AnalyticsCacheIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Account account;
    private Category incomeCategory;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {

        // Each test starts with an empty cache so previous test runs
        // cannot affect the cache-hit assertion.
        Cache cache = cacheManager.getCache("monthlySummary");

        if (cache != null) {
            cache.clear();
        }

        user = new User(
                "cache-test@example.com",
                "hashed-password",
                "Cache Test User"
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

        saveTransaction(
                TransactionType.INCOME,
                new BigDecimal("75000.00"),
                incomeCategory,
                OffsetDateTime.of(
                        2026, 9, 5, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );

        saveTransaction(
                TransactionType.EXPENSE,
                new BigDecimal("42000.00"),
                expenseCategory,
                OffsetDateTime.of(
                        2026, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC
                )
        );
    }

    @Test
    void getMonthlySummary_shouldCacheRepeatedRequest() {

        // The first call calculates the monthly summary from PostgreSQL.
        var firstResult =
                analyticsService.getMonthlySummary(
                        user.getId(),
                        2026,
                        9
                );

        // The second identical call should be served from Redis.
        var secondResult =
                analyticsService.getMonthlySummary(
                        user.getId(),
                        2026,
                        9
                );

        assertThat(firstResult)
                .isEqualTo(secondResult);

        assertThat(firstResult.totalIncome())
                .isEqualByComparingTo("75000.00");

        assertThat(firstResult.totalExpenses())
                .isEqualByComparingTo("42000.00");

        assertThat(firstResult.netSavings())
                .isEqualByComparingTo("33000.00");

        // The cache should now contain the monthly summary.
        Cache cache = cacheManager.getCache("monthlySummary");

        assertThat(cache).isNotNull();

        String cacheKey =
                user.getId() + ":2026:9";

        assertThat(cache.get(cacheKey))
                .isNotNull();
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
                "Cache test transaction",
                occurredAt
        );

        return transactionRepository.save(transaction);
    }
}
