package com.kartik.finance_tracker.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;

@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_unit", nullable = false, length = 20)
    private RecurringFrequencyUnit frequencyUnit;

    @Column(name = "frequency_interval", nullable = false)
    private int frequencyInterval;

    @Column(name = "next_occurrence", nullable = false)
    private LocalDate nextOccurrence;

    @Column(name = "paused_until")
    private LocalDate pausedUntil;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected RecurringTransaction() {}

    public RecurringTransaction(
            User user,
            Account account,
            Category category,
            TransactionType type,
            BigDecimal amount,
            String description,
            RecurringFrequencyUnit frequencyUnit,
            int frequencyInterval,
            LocalDate nextOccurrence
    ) {
        this.user = user;
        this.account = account;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.frequencyUnit = frequencyUnit;
        this.frequencyInterval = frequencyInterval;
        this.nextOccurrence = nextOccurrence;
        this.pausedUntil = null;
        this.active = true;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Account getAccount() {
        return account;
    }

    public Category getCategory() {
        return category;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public RecurringFrequencyUnit getFrequencyUnit() {
        return frequencyUnit;
    }

    public int getFrequencyInterval() {
        return frequencyInterval;
    }

    public LocalDate getNextOccurrence() {
        return nextOccurrence;
    }

    public LocalDate getPausedUntil() {
        return pausedUntil;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /*
     * Updates the recurring rule for future generated transactions.
     * Existing transactions remain unchanged.
     */
    public void update(
            Account account,
            Category category,
            TransactionType type,
            BigDecimal amount,
            String description,
            RecurringFrequencyUnit frequencyUnit,
            int frequencyInterval,
            LocalDate nextOccurrence
    ) {
        this.account = account;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.frequencyUnit = frequencyUnit;
        this.frequencyInterval = frequencyInterval;
        this.nextOccurrence = nextOccurrence;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Pausing moves the next occurrence to the resume date.
     * Missed occurrences during the pause are not generated later.
     */
    public void pause(LocalDate pausedUntil) {
        this.pausedUntil = pausedUntil;
        this.nextOccurrence = pausedUntil;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Resuming clears the temporary pause and allows future
     * occurrences to be generated again.
     */
    public void resume() {
        this.pausedUntil = null;
        this.active = true;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Deactivation permanently stops future generation while
     * keeping the recurring rule available until it is deleted.
     */
    public void deactivate() {
        this.active = false;
        this.pausedUntil = null;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
