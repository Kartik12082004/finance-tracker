package com.kartik.finance_tracker.transactions;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByUser_Id(UUID userId);

    List<Transaction> findAllByAccount_IdOrDestinationAccount_Id(
            UUID accountId,
            UUID destinationAccountId
    );

    @Query("""
            SELECT
                COALESCE(SUM(CASE WHEN t.type = :incomeType THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = :expenseType THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
            AND t.occurredAt >= :start
            AND t.occurredAt < :end
            """)
    Object[] findMonthlyIncomeAndExpenses(
            UUID userId,
            TransactionType incomeType,
            TransactionType expenseType,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Query("""
            SELECT
                c.id,
                c.name,
                SUM(t.amount)
            FROM Transaction t
            JOIN t.category c
            WHERE t.user.id = :userId
            AND t.type = :expenseType
            AND t.occurredAt >= :start
            AND t.occurredAt < :end
            GROUP BY c.id, c.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> findSpendingByCategory(
            UUID userId,
            TransactionType expenseType,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Query("""
            SELECT
                EXTRACT(MONTH FROM t.occurredAt),
                COALESCE(SUM(CASE WHEN t.type = :incomeType THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = :expenseType THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
            AND t.occurredAt >= :start
            AND t.occurredAt < :end
            GROUP BY EXTRACT(MONTH FROM t.occurredAt)
            ORDER BY EXTRACT(MONTH FROM t.occurredAt)
           """)
    List<Object[]> findIncomeVsExpenseByMonth(
            UUID userId,
            TransactionType incomeType,
            TransactionType expenseType,
            OffsetDateTime start,
            OffsetDateTime end
   );

}
