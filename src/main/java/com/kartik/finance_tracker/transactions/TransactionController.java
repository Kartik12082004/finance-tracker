package com.kartik.finance_tracker.transactions;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.security.CurrentUserService;
import com.kartik.finance_tracker.transactions.dto.CreateTransactionRequest;
import com.kartik.finance_tracker.transactions.dto.TransactionResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    public TransactionController(
            TransactionService transactionService,
            CurrentUserService currentUserService
    ) {
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        Transaction transaction = transactionService.createTransaction(
                userId,
                request.accountId(),
                request.destinationAccountId(),
                request.categoryId(),
                request.type(),
                request.amount(),
                request.description(),
                request.occurredAt()
        );

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getDestinationAccount() != null
                        ? transaction.getDestinationAccount().getId()
                        : null,
                transaction.getCategory() != null
                        ? transaction.getCategory().getId()
                        : null,
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getOccurredAt(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    @GetMapping
    public List<TransactionResponse> getTransactions() {
        UUID userId = currentUserService.getCurrentUserId();

        return transactionService.getTransactions(userId)
                .stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getAccount().getId(),
                        transaction.getDestinationAccount() != null
                                ? transaction.getDestinationAccount().getId()
                                : null,
                        transaction.getCategory() != null
                                ? transaction.getCategory().getId()
                                : null,
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getDescription(),
                        transaction.getOccurredAt(),
                        transaction.getCreatedAt(),
                        transaction.getUpdatedAt()
                ))
                .toList();
    }
}
