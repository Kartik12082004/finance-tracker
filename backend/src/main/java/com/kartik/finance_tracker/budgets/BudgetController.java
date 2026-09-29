package com.kartik.finance_tracker.budgets;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.budgets.dto.BudgetResponse;
import com.kartik.finance_tracker.budgets.dto.CreateBudgetRequest;
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
            @Valid @RequestBody CreateBudgetRequest request
    ) {
        // The authenticated user's ID comes from Spring Security.
        // The client never supplies the budget owner.
        UUID userId = currentUserService.getCurrentUserId();

        BudgetResponse response = budgetService.createBudget(
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        /*
         * Without a date, return all budgets belonging to the authenticated user.
         * With a date, return only budgets active on that date.
         */
        List<BudgetResponse> budgets = date == null
                ? budgetService.getBudgets(userId)
                : budgetService.getBudgetsForDate(userId, date);

        return ResponseEntity.ok(budgets);
    }
}
