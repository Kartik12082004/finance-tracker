package com.kartik.finance_tracker.budgets;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUser_Id(UUID userId);

    List<Budget> findByUser_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID userId,
            LocalDate date,
            LocalDate sameDate
    );

    boolean existsByUser_IdAndCategory_IdAndPeriodAndStartDateAndEndDate(
            UUID userId,
            UUID categoryId,
            BudgetPeriod period,
            LocalDate startDate,
            LocalDate endDate
    );
}
