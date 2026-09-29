package com.kartik.finance_tracker.categories.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.categories.CategoryType;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        UUID parentId,
        boolean isDefault,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}