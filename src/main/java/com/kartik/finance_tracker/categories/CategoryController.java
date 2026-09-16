package com.kartik.finance_tracker.categories;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.categories.dto.CategoryResponse;
import com.kartik.finance_tracker.categories.dto.CreateCategoryRequest;
import com.kartik.finance_tracker.security.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public CategoryController(
            CategoryService categoryService,
            CurrentUserService currentUserService
    ) {
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

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
    public List<CategoryResponse> getCategories() {
        UUID userId = currentUserService.getCurrentUserId();

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
