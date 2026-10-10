
package com.kartik.finance_tracker.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
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

    @Column(nullable = false, precision = 19, scale = 2)
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

    /*
     * Preserves the original calendar day for monthly/yearly
     * recurring transactions.
     */
    @Column(name = "anchor_day", nullable = false)
    private int anchorDay;

    @Column(name = "skip_next_occurrence", nullable = false)
    private boolean skipNextOccurrence;

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
        this.anchorDay = nextOccurrence.getDayOfMonth();
        this.skipNextOccurrence = false;
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

    public int getAnchorDay() {
        return anchorDay;
    }

    public boolean isSkipNextOccurrence() {
        return skipNextOccurrence;
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
        this.anchorDay = nextOccurrence.getDayOfMonth();
        this.skipNextOccurrence = false;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Marks the currently scheduled occurrence to be skipped.
     * The date remains unchanged so the user can undo the skip
     * before the occurrence is due.
     */
    public void skipNextOccurrence() {
        this.skipNextOccurrence = true;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void undoSkipNextOccurrence() {
        this.skipNextOccurrence = false;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Called by the generation service when nextOccurrence is due.
     * Returns true if this occurrence should be skipped.
     */
    public boolean consumeSkipForDueOccurrence() {
        if (!skipNextOccurrence) {
            return false;
        }

        this.skipNextOccurrence = false;
        advanceNextOccurrence();
        return true;
    }

    private LocalDate calculateOccurrenceAfter(int intervals) {
        return switch (frequencyUnit) {
            case WEEK -> nextOccurrence.plusWeeks(intervals);

            case MONTH -> {
                YearMonth targetMonth =
                        YearMonth.from(nextOccurrence).plusMonths(intervals);
                int day = Math.min(anchorDay, targetMonth.lengthOfMonth());
                yield targetMonth.atDay(day);
            }

            case YEAR -> {
                YearMonth targetMonth =
                        YearMonth.from(nextOccurrence).plusYears(intervals);
                int day = Math.min(anchorDay, targetMonth.lengthOfMonth());
                yield targetMonth.atDay(day);
            }
        };
    }

    public void advanceNextOccurrence() {
        this.nextOccurrence =
                calculateOccurrenceAfter(frequencyInterval);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
