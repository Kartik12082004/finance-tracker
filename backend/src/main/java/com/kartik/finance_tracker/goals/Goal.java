package com.kartik.finance_tracker.goals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.UUID;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.users.User;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentAmount;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Goal() {
        // Required by JPA
    }

    public Goal(
            User user,
            String name,
            BigDecimal targetAmount,
            LocalDate targetDate
    ) {
        this.user = user;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = BigDecimal.ZERO;
        this.targetDate = targetDate;
        this.status = GoalStatus.ACTIVE;
        this.archived = false;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void update(
            String name,
            BigDecimal targetAmount,
            LocalDate targetDate
    ) {
        if (this.status == GoalStatus.COMPLETED
                || this.status == GoalStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Completed or cancelled goals cannot be updated"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Goal name is required"
            );
        }

        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Target amount must be greater than zero"
            );
        }

        if (targetAmount.compareTo(this.currentAmount) < 0) {
            throw new IllegalArgumentException(
                    "Target amount cannot be less than current amount"
            );
        }

        this.name = name;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;

        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void synchronizeBalance(BigDecimal balance) {
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException(
                    "Goal balance cannot be negative or null"
            );
        }

        if (balance.compareTo(targetAmount) > 0) {
            throw new IllegalArgumentException(
                    "Goal balance cannot exceed target amount"
            );
        }

        this.currentAmount = balance;

        if (this.status != GoalStatus.CANCELLED
                && this.status != GoalStatus.PAUSED) {
            this.status = balance.compareTo(targetAmount) == 0
                    ? GoalStatus.COMPLETED
                    : GoalStatus.ACTIVE;
        }

        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void pause() {
        if (this.status != GoalStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active goals can be paused"
            );
        }

        this.status = GoalStatus.PAUSED;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void resume() {
        if (this.status != GoalStatus.PAUSED) {
            throw new IllegalStateException(
                    "Only paused goals can be resumed"
            );
        }

        this.status = this.currentAmount.compareTo(this.targetAmount) == 0
                ? GoalStatus.COMPLETED
                : GoalStatus.ACTIVE;

        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void cancel() {
        if (this.status == GoalStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed goals cannot be cancelled"
            );
        }

        if (this.status == GoalStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Goal is already cancelled"
            );
        }

        this.status = GoalStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void archive() {
        if (this.status != GoalStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only completed goals can be archived"
            );
        }

        if (this.archived) {
            throw new IllegalStateException(
                    "Goal is already archived"
            );
        }

        this.archived = true;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void unarchive() {
        if (!this.archived) {
            throw new IllegalStateException(
                    "Goal is not archived"
            );
        }

        this.archived = false;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
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

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public boolean isArchived() {
        return archived;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
