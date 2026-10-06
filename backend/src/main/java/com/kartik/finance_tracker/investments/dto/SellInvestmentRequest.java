package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SellInvestmentRequest(

        @NotNull
        UUID accountId,

        @NotNull
        @DecimalMin("0.00000001")
        BigDecimal quantity,

        @NotNull
        OffsetDateTime occurredAt
) {
}
