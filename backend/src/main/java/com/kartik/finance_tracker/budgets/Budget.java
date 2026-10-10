package com.kartik.finance_tracker.budgets;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.users.User;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "budget_year", nullable = false)
    private int year;

    @Column(name = "budget_month", nullable = false)
    private int month;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Budget() {
    }

    public Budget(
            User user,
            Category category,
            BigDecimal amount,
            int year,
            int month
    ) {
        this.user = user;
        this.category = category;
        this.amount = amount;
        this.year = year;
        this.month = month;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
