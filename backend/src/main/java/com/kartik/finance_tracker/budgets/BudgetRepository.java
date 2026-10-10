package com.kartik.finance_tracker.budgets;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUser_IdAndYearAndMonth(
            UUID userId,
            int year,
            int month
    );

    List<Budget> findByUser_Id(UUID userId);

    List<Budget> findByUser_IdAndCategory_IdOrderByYearDescMonthDesc(
            UUID userId,
            UUID categoryId
    );

    boolean existsByUser_IdAndCategory_IdAndYearAndMonth(
            UUID userId,
            UUID categoryId,
            int year,
            int month
    );

    Optional<Budget> findByIdAndUser_Id(
            UUID budgetId,
            UUID userId
    );
}
