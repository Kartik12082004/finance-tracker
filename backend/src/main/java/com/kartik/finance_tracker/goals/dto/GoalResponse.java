package com.kartik.finance_tracker.goals.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.goals.GoalStatus;

public record GoalResponse(
        UUID id,
        String name,
        UUID accountId,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        GoalStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
