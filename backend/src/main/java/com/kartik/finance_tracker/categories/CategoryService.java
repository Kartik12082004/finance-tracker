package com.kartik.finance_tracker.categories;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.categories.dto.UpdateCategoryRequest;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Category createCategory(
            UUID userId,
            String name,
            CategoryType type,
            UUID parentId,
            boolean isDefault
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Category parent = resolveParent(
                userId,
                null,
                parentId,
                type
        );

        Category category = new Category(
                user,
                name,
                type,
                parent,
                isDefault
        );

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(
            UUID userId,
            UUID categoryId,
            UpdateCategoryRequest request
    ) {
        Category category = getOwnedCategory(userId, categoryId);

        if (category.isDefault()) {
            throw new IllegalArgumentException(
                    "Default categories cannot be edited"
            );
        }

        Category parent = resolveParent(
                userId,
                categoryId,
                request.parentId(),
                category.getType()
        );

        category.update(
                request.name().trim(),
                category.getType(),
                parent
        );

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(
            UUID userId,
            UUID categoryId
    ) {
        Category category = getOwnedCategory(userId, categoryId);

        if (category.isDefault()) {
            throw new IllegalArgumentException(
                    "Default categories cannot be deleted"
            );
        }

        if (categoryRepository.existsByParent_Id(categoryId)) {
            throw new IllegalArgumentException(
                    "Category with child categories cannot be deleted"
            );
        }

        if (transactionRepository.existsByCategory_Id(categoryId)) {
            throw new IllegalArgumentException(
                    "Category with transaction history cannot be deleted"
            );
        }

        categoryRepository.delete(category);
    }

    public void ensureDefaultCategories(User user) {
        List<Category> existingCategories =
                categoryRepository.findAllByUser_Id(user.getId());

        // If default categories already exist, do not create duplicates.
        boolean hasDefaultCategories = existingCategories.stream()
                .anyMatch(Category::isDefault);

        if (hasDefaultCategories) {
            return;
        }

        // Expense categories
        Category food = createDefaultCategory(user, "Food", CategoryType.EXPENSE);
        createDefaultSubcategory(user, "Groceries", CategoryType.EXPENSE, food);
        createDefaultSubcategory(user, "Restaurants", CategoryType.EXPENSE, food);
        createDefaultSubcategory(user, "Delivery", CategoryType.EXPENSE, food);

        Category housing = createDefaultCategory(user, "Housing", CategoryType.EXPENSE);
        createDefaultSubcategory(user, "Rent", CategoryType.EXPENSE, housing);
        createDefaultSubcategory(user, "Electricity", CategoryType.EXPENSE, housing);
        createDefaultSubcategory(user, "Maintenance", CategoryType.EXPENSE, housing);

        Category transport = createDefaultCategory(user, "Transport", CategoryType.EXPENSE);
        createDefaultSubcategory(user, "Fuel", CategoryType.EXPENSE, transport);
        createDefaultSubcategory(user, "Public Transport", CategoryType.EXPENSE, transport);
        createDefaultSubcategory(user, "Cab", CategoryType.EXPENSE, transport);

        Category shopping = createDefaultCategory(user, "Shopping", CategoryType.EXPENSE);
        createDefaultSubcategory(user, "Clothing", CategoryType.EXPENSE, shopping);
        createDefaultSubcategory(user, "Electronics", CategoryType.EXPENSE, shopping);
        createDefaultSubcategory(user, "General", CategoryType.EXPENSE, shopping);

        Category entertainment =
                createDefaultCategory(user, "Entertainment", CategoryType.EXPENSE);
        createDefaultSubcategory(user, "Movies", CategoryType.EXPENSE, entertainment);
        createDefaultSubcategory(user, "Games", CategoryType.EXPENSE, entertainment);
        createDefaultSubcategory(user, "Streaming", CategoryType.EXPENSE, entertainment);

        createDefaultCategory(user, "Miscellaneous", CategoryType.EXPENSE);

        // Income categories
        createDefaultCategory(user, "Salary", CategoryType.INCOME);
        createDefaultCategory(user, "Freelance", CategoryType.INCOME);
        createDefaultCategory(user, "Interest", CategoryType.INCOME);
        createDefaultCategory(user, "Refunds", CategoryType.INCOME);
        createDefaultCategory(user, "Other Income", CategoryType.INCOME);
    }

    private Category createDefaultCategory(
            User user, String name, CategoryType type) {
        Category category = new Category(user, name, type, null, true);
        return categoryRepository.save(category);
    }

    private Category createDefaultSubcategory(
            User user, String name, CategoryType type, Category parent) {
        Category category = new Category(user, name, type, parent, true);
        return categoryRepository.save(category);
    }


    @Transactional(readOnly = true)
    public List<Category> getCategories(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return categoryRepository.findAllByUser_Id(userId);
    }

    private Category getOwnedCategory(
            UUID userId,
            UUID categoryId
    ) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"
                        ));

        if (!category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(
                    "Category not found"
            );
        }

        return category;
    }

    private Category resolveParent(
            UUID userId,
            UUID categoryId,
            UUID parentId,
            CategoryType type
    ) {
        if (parentId == null) {
            return null;
        }

        if (categoryId != null && categoryId.equals(parentId)) {
            throw new IllegalArgumentException(
                    "A category cannot be its own parent"
            );
        }

        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent category not found"
                        ));

        if (!parent.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(
                    "Parent category not found"
            );
        }

        if (parent.getType() != type) {
            throw new IllegalArgumentException(
                    "Category type must match parent category type"
            );
        }

        if (categoryId != null
                && isDescendantOf(parent, categoryId)) {
            throw new IllegalArgumentException(
                    "A category cannot be moved under one of its descendants"
            );
        }

        return parent;
    }

    private boolean isDescendantOf(
            Category candidateParent,
            UUID categoryId
    ) {
        Category current = candidateParent;

        while (current != null) {
            if (current.getId().equals(categoryId)) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }
}
