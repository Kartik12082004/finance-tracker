package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BuyInvestmentRequest(

        @NotNull
        UUID accountId,

        @NotNull
        @DecimalMin("0.00000001")
        BigDecimal quantity,

        @NotNull
        @DecimalMin("0.0001")
        BigDecimal pricePerUnit,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        OffsetDateTime occurredAt
) {
}
