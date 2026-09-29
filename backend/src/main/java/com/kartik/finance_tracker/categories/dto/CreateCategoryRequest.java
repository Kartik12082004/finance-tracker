package com.kartik.finance_tracker.categories.dto;

import java.util.UUID;

import com.kartik.finance_tracker.categories.CategoryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @NotNull(message = "Category type is required")
        CategoryType type,

        UUID parentId
) {
}
