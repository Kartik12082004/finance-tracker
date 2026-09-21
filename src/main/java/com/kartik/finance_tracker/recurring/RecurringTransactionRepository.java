package com.kartik.finance_tracker.recurring;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, UUID> {

    List<RecurringTransaction> findByUser_Id(UUID userId);

    Optional<RecurringTransaction> findByIdAndUser_Id(
            UUID id,
            UUID userId
    );

    @Query("""
            SELECT r
            FROM RecurringTransaction r
            WHERE r.active = true
              AND r.nextOccurrence <= :date
              AND (
                    r.pausedUntil IS NULL
                    OR r.pausedUntil <= :date
                  )
            """)
    List<RecurringTransaction> findDueRecurringTransactions(
            @Param("date") LocalDate date
    );
}
