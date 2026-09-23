package com.kartik.finance_tracker.investments;

import java.math.BigDecimal;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "investment_transactions")
public class InvestmentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Every investment transaction belongs to exactly one user.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /*
     * For BUY this is the account money is taken from.
     * For SELL this is the account receiving the money.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InvestmentTransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "price_per_unit", precision = 19, scale = 4)
    private BigDecimal pricePerUnit;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public InvestmentTransaction(
            User user,
            Investment investment,
            Account account,
            InvestmentTransactionType type,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal pricePerUnit,
            OffsetDateTime occurredAt
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.investment = investment;
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.occurredAt = occurredAt;

        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
