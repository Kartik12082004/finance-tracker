package com.kartik.finance_tracker.transactions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.users.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "transactions")
public class Transaction {

    public Transaction(
            User user,
            Account account,
            Account destinationAccount,
            Category category,
            TransactionType type,
            BigDecimal amount,
            String description,
            OffsetDateTime occurredAt
    ) {
        // Generate the transaction ID in the application.
        this.id = UUID.randomUUID();

        this.user = user;
        this.account = account;
        this.destinationAccount = destinationAccount;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.occurredAt = occurredAt;

        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @Id
    private UUID id;

    // Every transaction belongs to a specific user.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The account from which money originates or where an expense/income occurs.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /*
     * Only transfers have a destination account.
     * Income and expense transactions leave this field null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    /*
     * Income and expense transactions use a category.
     * Transfers deliberately leave this field null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    // When the financial event actually occurred.
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    // Timestamps for tracking the transaction record itself.
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}