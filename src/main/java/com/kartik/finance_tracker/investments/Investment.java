package com.kartik.finance_tracker.investments;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Every investment belongs to exactly one user.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestmentType type;

    /*
     * Quantity and average purchase price are optional because
     * V1 does not require unit-level tracking for every investment.
     */
    @Column(precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_purchase_price", precision = 19, scale = 4)
    private BigDecimal averagePurchasePrice;

    /*
     * Current value is user-updatable until live market data
     * is introduced in a later phase.
     */
    @Column(
        name = "current_value",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal currentValue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Investment(
            User user,
            String name,
            InvestmentType type
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.name = name;
        this.type = type;
        this.currentValue = BigDecimal.ZERO;

        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    public void update(
            String name,
            InvestmentType type,
            BigDecimal currentValue
    ) {
        this.name = name;
        this.type = type;
        this.currentValue = currentValue;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setAveragePurchasePrice(
                BigDecimal averagePurchasePrice
        ) {
            this.averagePurchasePrice = averagePurchasePrice;
            this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
        public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

}
