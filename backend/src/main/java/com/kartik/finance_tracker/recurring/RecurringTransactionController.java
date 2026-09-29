package com.kartik.finance_tracker.recurring;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.recurring.dto.CreateRecurringTransactionRequest;
import com.kartik.finance_tracker.recurring.dto.RecurringTransactionResponse;
import com.kartik.finance_tracker.recurring.dto.UpdateRecurringTransactionRequest;
import com.kartik.finance_tracker.security.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;
    private final CurrentUserService currentUserService;

    public RecurringTransactionController(
            RecurringTransactionService recurringTransactionService,
            CurrentUserService currentUserService
    ) {
        this.recurringTransactionService = recurringTransactionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(
            @Valid @RequestBody CreateRecurringTransactionRequest request
    ) {
        // The authenticated user owns the recurring transaction.
        UUID userId = currentUserService.getCurrentUserId();

        RecurringTransactionResponse response =
                recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>> getRecurringTransactions() {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                recurringTransactionService.getRecurringTransactions(userId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> updateRecurringTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringTransactionRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                recurringTransactionService.updateRecurringTransaction(
                        userId,
                        id,
                        request
                )
        );
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringTransactionResponse> pauseRecurringTransaction(
            @PathVariable UUID id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate pausedUntil
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                recurringTransactionService.pauseRecurringTransaction(
                        userId,
                        id,
                        pausedUntil
                )
        );
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringTransactionResponse> resumeRecurringTransaction(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                recurringTransactionService.resumeRecurringTransaction(
                        userId,
                        id
                )
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<RecurringTransactionResponse> deactivateRecurringTransaction(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                recurringTransactionService.deactivateRecurringTransaction(
                        userId,
                        id
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(
            @PathVariable UUID id
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        recurringTransactionService.deleteRecurringTransaction(
                userId,
                id
        );

        return ResponseEntity.noContent().build();
    }
}
