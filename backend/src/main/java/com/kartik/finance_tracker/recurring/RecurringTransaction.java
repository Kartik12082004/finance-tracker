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
     *
     * Example:
     * January 31 -> February 28 -> March 31
     */
    @Column(name = "anchor_day", nullable = false)
    private int anchorDay;

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

        /*
         * The original day becomes the recurrence anchor.
         *
         * Example:
         * 31st -> February 28th -> March 31st
         */
        this.anchorDay = nextOccurrence.getDayOfMonth();

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

    public int getAnchorDay() {
        return anchorDay;
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
     *
     * A new next occurrence establishes a new calendar-day anchor.
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

        /*
         * Editing the next occurrence also establishes a new
         * recurrence anchor.
         */
        this.anchorDay = nextOccurrence.getDayOfMonth();

        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Temporarily pauses the recurring rule.
     *
     * The duration uses the recurring rule's frequency unit:
     *
     * WEEK  -> weeks
     * MONTH -> months
     * YEAR  -> years
     *
     * Missed occurrences during the pause are not generated later.
     */
    public void pauseFor(int duration) {
        LocalDate resumeDate = calculateOccurrenceAfter(duration);

        this.pausedUntil = resumeDate;
        this.nextOccurrence = resumeDate;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Pauses the recurring rule indefinitely.
     *
     * active=false ensures the scheduler ignores the rule until
     * the user explicitly resumes it.
     */
    public void pauseUntilResumed() {
        this.active = false;
        this.pausedUntil = null;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Resumes the recurring rule.
     *
     * If the stored next occurrence is already in the past,
     * missed occurrences are skipped instead of being replayed.
     */
    public void resume(LocalDate today) {
        this.active = true;
        this.pausedUntil = null;

        while (!nextOccurrence.isAfter(today)) {
            advanceNextOccurrence();
        }

        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /*
     * Calculates a future occurrence using the recurring rule's
     * frequency unit.
     *
     * Monthly/yearly schedules preserve the original anchor day,
     * even when the target month is shorter.
     */
    private LocalDate calculateOccurrenceAfter(int intervals) {
        return switch (frequencyUnit) {

            case WEEK ->
                    nextOccurrence.plusWeeks(intervals);

            case MONTH -> {
                YearMonth targetMonth =
                        YearMonth.from(nextOccurrence)
                                .plusMonths(intervals);

                int day = Math.min(
                        anchorDay,
                        targetMonth.lengthOfMonth()
                );

                yield targetMonth.atDay(day);
            }

            case YEAR -> {
                YearMonth targetMonth =
                        YearMonth.from(nextOccurrence)
                                .plusYears(intervals);

                int day = Math.min(
                        anchorDay,
                        targetMonth.lengthOfMonth()
                );

                yield targetMonth.atDay(day);
            }
        };
    }

    /*
     * Advances to the next normal scheduled occurrence.
     */
    public void advanceNextOccurrence() {
        this.nextOccurrence =
                calculateOccurrenceAfter(frequencyInterval);

        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
