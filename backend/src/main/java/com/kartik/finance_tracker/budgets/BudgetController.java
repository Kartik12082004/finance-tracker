package com.kartik.finance_tracker.budgets;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.budgets.dto.BudgetResponse;
import com.kartik.finance_tracker.budgets.dto.CreateBudgetRequest;
import com.kartik.finance_tracker.budgets.dto.UpdateBudgetRequest;
import com.kartik.finance_tracker.security.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserService currentUserService;

    public BudgetController(
            BudgetService budgetService,
            CurrentUserService currentUserService
    ) {
        this.budgetService = budgetService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @RequestParam int year,
            @RequestParam int month,
            @Valid @RequestBody CreateBudgetRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        BudgetResponse response = budgetService.createBudget(
                userId,
                year,
                month,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @RequestParam int year,
            @RequestParam int month
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        List<BudgetResponse> budgets =
                budgetService.getBudgetsForMonth(userId, year, month);

        return ResponseEntity.ok(budgets);
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable UUID budgetId,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        BudgetResponse response = budgetService.updateBudget(
                userId,
                budgetId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable UUID budgetId
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        budgetService.deleteBudget(userId, budgetId);

        return ResponseEntity.noContent().build();
    }
}
