package com.kartik.finance_tracker.categories;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.categories.dto.CategoryResponse;
import com.kartik.finance_tracker.categories.dto.CreateCategoryRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        // X-User-Id is temporary as of right now, since we don't have authentication implemented yet.
        // Authentication will provide the user identity once security is implemented.
        Category category = categoryService.createCategory(
                userId,
                request.name(),
                request.type(),
                request.parentId(),
                request.isDefault()
        );

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getParent() != null
                        ? category.getParent().getId()
                        : null,
                category.isDefault(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    @GetMapping
    public List<CategoryResponse> getCategories(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        // X-User-Id is temporary as of right now, since we don't have authentication implemented yet.
        // Authentication will provide the user identity once security is implemented.
        return categoryService.getCategories(userId)
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getType(),
                        category.getParent() != null
                                ? category.getParent().getId()
                                : null,
                        category.isDefault(),
                        category.getCreatedAt(),
                        category.getUpdatedAt()
                ))
                .toList();
    }
}