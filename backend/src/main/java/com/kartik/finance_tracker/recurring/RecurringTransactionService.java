package com.kartik.finance_tracker.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.recurring.dto.CreateRecurringTransactionRequest;
import com.kartik.finance_tracker.recurring.dto.RecurringTransactionResponse;
import com.kartik.finance_tracker.recurring.dto.UpdateRecurringTransactionRequest;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringTransactionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository
    ) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(
            UUID userId,
            CreateRecurringTransactionRequest request
    ) {
        User user = getUser(userId);

        Account account =
                getAccountForUser(request.accountId(), userId);

        Category category =
                getCategoryForUser(request.categoryId(), userId);

        validateTransactionType(request.type());
        validateAmountScale(request.amount());
        validateNextOccurrence(request.nextOccurrence());

        RecurringTransaction recurringTransaction =
                new RecurringTransaction(
                        user,
                        account,
                        category,
                        request.type(),
                        request.amount(),
                        request.description(),
                        request.frequencyUnit(),
                        request.frequencyInterval(),
                        request.nextOccurrence()
                );

        return toResponse(
                recurringTransactionRepository.save(recurringTransaction)
        );
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getRecurringTransactions(
            UUID userId
    ) {
        getUser(userId);

        return recurringTransactionRepository
                .findByUser_Id(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(
            UUID userId,
            UUID recurringTransactionId,
            UpdateRecurringTransactionRequest request
    ) {
        RecurringTransaction recurringTransaction =
                getRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        Account account =
                getAccountForUser(request.accountId(), userId);

        Category category =
                getCategoryForUser(request.categoryId(), userId);

        validateTransactionType(request.type());
        validateAmountScale(request.amount());
        validateNextOccurrence(request.nextOccurrence());

        /*
         * Editing a recurring rule only changes future generated
         * transactions.
         *
         * Transactions that were already generated remain unchanged.
         */
        recurringTransaction.update(
                account,
                category,
                request.type(),
                request.amount(),
                request.description(),
                request.frequencyUnit(),
                request.frequencyInterval(),
                request.nextOccurrence()
        );

        return toResponse(
                recurringTransactionRepository.save(recurringTransaction)
        );
    }

    @Transactional
    public RecurringTransactionResponse skipNextOccurrence(
                UUID userId,
                UUID recurringTransactionId
        ) {
        RecurringTransaction recurringTransaction =
                getRecurringTransaction(userId, recurringTransactionId);

        if (recurringTransaction.isSkipNextOccurrence()) {
                throw new IllegalArgumentException(
                        "The next occurrence is already marked to be skipped"
                );
        }

        if (!recurringTransaction.isActive()) {
                throw new IllegalArgumentException(
                        "Cannot skip an occurrence while the recurring transaction is inactive"
                );
        }

        if (!recurringTransaction.getNextOccurrence().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Cannot skip an occurrence that is already due"
                );
        }

        recurringTransaction.skipNextOccurrence();

        return toResponse(
                recurringTransactionRepository.save(recurringTransaction)
        );
   }

   @Transactional
   public RecurringTransactionResponse undoSkipNextOccurrence(
                UUID userId,
                UUID recurringTransactionId
        ) {
        RecurringTransaction recurringTransaction =
                getRecurringTransaction(userId, recurringTransactionId);

        if (!recurringTransaction.isSkipNextOccurrence()) {
                throw new IllegalArgumentException(
                        "The next occurrence is not marked to be skipped"
                );
        }

        if (!recurringTransaction.getNextOccurrence().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Cannot undo a skip once the occurrence is due"
                );
        }

        recurringTransaction.undoSkipNextOccurrence();

        return toResponse(
                recurringTransactionRepository.save(recurringTransaction)
        );
    }

    @Transactional
    public void deleteRecurringTransaction(
            UUID userId,
            UUID recurringTransactionId
    ) {
        RecurringTransaction recurringTransaction =
                getRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        /*
         * Deleting the recurring rule must never delete historical
         * Transaction records generated from it.
         */
        recurringTransactionRepository.delete(recurringTransaction);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }

    private Account getAccountForUser(
            UUID accountId,
            UUID userId
    ) {
        Account account =
                accountRepository.findById(accountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                ));

        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Account does not belong to the authenticated user"
            );
        }

        return account;
    }

    private Category getCategoryForUser(
            UUID categoryId,
            UUID userId
    ) {
        Category category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                ));

        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Category does not belong to the authenticated user"
            );
        }

        return category;
    }

    private RecurringTransaction getRecurringTransaction(
            UUID userId,
            UUID recurringTransactionId
    ) {
        return recurringTransactionRepository
                .findByIdAndUser_Id(
                        recurringTransactionId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Recurring transaction not found"
                        ));
    }

    private void validateTransactionType(TransactionType type) {
        if (type == TransactionType.TRANSFER) {
            throw new IllegalArgumentException(
                    "Recurring transactions cannot be transfers"
            );
        }
    }

    private void validateAmountScale(BigDecimal amount) {
        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }

    private void validateNextOccurrence(LocalDate nextOccurrence) {
        if (nextOccurrence == null) {
                throw new IllegalArgumentException(
                        "Payment date is required"
                );
        }

        if (nextOccurrence.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Payment date cannot be in the past"
                );
        }
    }


    private RecurringTransactionResponse toResponse(
            RecurringTransaction recurringTransaction
    ) {
        return new RecurringTransactionResponse(
                recurringTransaction.getId(),
                recurringTransaction.getAccount().getId(),
                recurringTransaction.getCategory() != null
                        ? recurringTransaction.getCategory().getId()
                        : null,
                recurringTransaction.getCategory() != null
                        ? recurringTransaction.getCategory().getName()
                        : null,
                recurringTransaction.getType(),
                recurringTransaction.getAmount(),
                recurringTransaction.getDescription(),
                recurringTransaction.getFrequencyUnit(),
                recurringTransaction.getFrequencyInterval(),
                recurringTransaction.getNextOccurrence(),
                recurringTransaction.isSkipNextOccurrence(),
                recurringTransaction.isActive(),
                recurringTransaction.getCreatedAt(),
                recurringTransaction.getUpdatedAt()
        );
    }
}
