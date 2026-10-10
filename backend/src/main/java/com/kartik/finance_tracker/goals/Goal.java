package com.kartik.finance_tracker.goals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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

        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void update(
            String name,
            BigDecimal targetAmount,
            LocalDate targetDate
    ) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Synchronizes persisted goal progress and completion status with the
     * actual balance of the associated Goal Account.
     */
    public void synchronizeBalance(BigDecimal balance) {
        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException(
                    "Goal account balance cannot be negative"
            );
        }

        if (balance.compareTo(targetAmount) > 0) {
            throw new IllegalArgumentException(
                    "Goal account balance cannot exceed the goal target"
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
            throw new IllegalArgumentException(
                    "Only active goals can be paused"
            );
        }

        this.status = GoalStatus.PAUSED;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void resume() {
        if (this.status != GoalStatus.PAUSED) {
            throw new IllegalArgumentException(
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
            throw new IllegalArgumentException(
                    "Completed goals cannot be cancelled"
            );
        }

        this.status = GoalStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
