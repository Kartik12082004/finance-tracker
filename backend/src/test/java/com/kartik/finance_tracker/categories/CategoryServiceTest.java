package com.kartik.finance_tracker.categories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.categories.dto.UpdateCategoryRequest;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

public class CategoryServiceTest {

    @Test
    void createCategory_shouldSaveAndReturnCategory() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        UUID userId = user.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        Category savedCategory = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        Category result = categoryService.createCategory(
                userId,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        assertThat(result).isSameAs(savedCategory);

        verify(userRepository).findById(userId);

        ArgumentCaptor<Category> categoryCaptor =
                ArgumentCaptor.forClass(Category.class);

        verify(categoryRepository).save(categoryCaptor.capture());

        Category categoryPassedToRepository =
                categoryCaptor.getValue();

        assertThat(categoryPassedToRepository.getUser())
                .isSameAs(user);

        assertThat(categoryPassedToRepository.getName())
                .isEqualTo("Food");

        assertThat(categoryPassedToRepository.getType())
                .isEqualTo(CategoryType.EXPENSE);

        assertThat(categoryPassedToRepository.getParent())
                .isNull();

        assertThat(categoryPassedToRepository.isDefault())
                .isTrue();

        assertThat(categoryPassedToRepository.getId())
                .isNotNull();

        assertThat(categoryPassedToRepository.getCreatedAt())
                .isNotNull();

        assertThat(categoryPassedToRepository.getUpdatedAt())
                .isNotNull();
    }

    @Test
    void createCategory_shouldCreateChildCategory() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        UUID userId = user.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        Category parent = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        Category savedCategory = new Category(
                user,
                "Restaurants",
                CategoryType.EXPENSE,
                parent,
                false
        );

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        Category result = categoryService.createCategory(
                userId,
                "Restaurants",
                CategoryType.EXPENSE,
                parent.getId(),
                false
        );

        assertThat(result).isSameAs(savedCategory);

        ArgumentCaptor<Category> categoryCaptor =
                ArgumentCaptor.forClass(Category.class);

        verify(categoryRepository).save(categoryCaptor.capture());

        Category categoryPassedToRepository =
                categoryCaptor.getValue();

        assertThat(categoryPassedToRepository.getUser())
                .isSameAs(user);

        assertThat(categoryPassedToRepository.getName())
                .isEqualTo("Restaurants");

        assertThat(categoryPassedToRepository.getParent())
                .isSameAs(parent);
    }

    @Test
    void createCategory_shouldThrowWhenParentTypeDoesNotMatch() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        UUID userId = user.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        Category parent = new Category(
                user,
                "Salary",
                CategoryType.INCOME,
                null,
                true
        );

        when(categoryRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        assertThatThrownBy(() ->
                categoryService.createCategory(
                        userId,
                        "Freelance",
                        CategoryType.EXPENSE,
                        parent.getId(),
                        false
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Category type must match parent category type"
                );
    }

    @Test
    void createCategory_shouldThrowWhenUserDoesNotExist() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                categoryService.createCategory(
                        userId,
                        "Food",
                        CategoryType.EXPENSE,
                        null,
                        true
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void createCategory_shouldThrowWhenParentDoesNotExist() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        UUID userId = user.getId();
        UUID parentId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findById(parentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                categoryService.createCategory(
                        userId,
                        "Restaurants",
                        CategoryType.EXPENSE,
                        parentId,
                        false
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Parent category not found");
    }

    @Test
    void createCategory_shouldThrowWhenParentBelongsToAnotherUser() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        User anotherUser = new User(
                "other@example.com",
                "hashed-password",
                "Other"
        );

        UUID userId = user.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        Category parent = new Category(
                anotherUser,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        assertThatThrownBy(() ->
                categoryService.createCategory(
                        userId,
                        "Restaurants",
                        CategoryType.EXPENSE,
                        parent.getId(),
                        false
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Parent category not found");
    }

    @Test
    void updateCategory_shouldUpdateCustomCategory() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        UUID userId = user.getId();

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                false
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(transactionRepository.existsByCategory_Id(category.getId()))
                .thenReturn(false);

        when(categoryRepository.save(category))
                .thenReturn(category);

        UpdateCategoryRequest request =
                new UpdateCategoryRequest(
                        "Dining",
                        null
                );

        Category result = categoryService.updateCategory(
                userId,
                category.getId(),
                request
        );

        assertThat(result).isSameAs(category);
        assertThat(category.getName()).isEqualTo("Dining");
        assertThat(category.getType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(category.getParent()).isNull();

        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_shouldUpdateParent() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        UUID userId = user.getId();

        Category category = new Category(
                user,
                "Restaurants",
                CategoryType.EXPENSE,
                null,
                false
        );

        Category parent = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(categoryRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        when(transactionRepository.existsByCategory_Id(category.getId()))
                .thenReturn(false);

        when(categoryRepository.save(category))
                .thenReturn(category);

        Category result = categoryService.updateCategory(
                userId,
                category.getId(),
                new UpdateCategoryRequest(
                        "Restaurants",
                        parent.getId()
                )
        );

        assertThat(result).isSameAs(category);
        assertThat(category.getParent()).isSameAs(parent);
    }

    @Test
    void updateCategory_shouldThrowWhenDefaultCategory() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() ->
                categoryService.updateCategory(
                        user.getId(),
                        category.getId(),
                        new UpdateCategoryRequest(
                                "Dining",
                                null
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Default categories cannot be edited");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_shouldThrowWhenCategoryBelongsToAnotherUser() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User owner = new User(
                "owner@example.com",
                "hashed-password",
                "Owner"
        );

        User anotherUser = new User(
                "other@example.com",
                "hashed-password",
                "Other"
        );

        Category category = new Category(
                owner,
                "Food",
                CategoryType.EXPENSE,
                null,
                false
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() ->
                categoryService.updateCategory(
                        anotherUser.getId(),
                        category.getId(),
                        new UpdateCategoryRequest(
                                "Dining",
                                null
                        )
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_shouldThrowWhenParentBelongsToAnotherUser() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        User anotherUser = new User(
                "other@example.com",
                "hashed-password",
                "Other"
        );

        Category category = new Category(
                user,
                "Restaurants",
                CategoryType.EXPENSE,
                null,
                false
        );

        Category parent = new Category(
                anotherUser,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(categoryRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        when(transactionRepository.existsByCategory_Id(category.getId()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                categoryService.updateCategory(
                        user.getId(),
                        category.getId(),
                        new UpdateCategoryRequest(
                                "Restaurants",
                                parent.getId()
                        )
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Parent category not found");
    }

    @Test
    void updateCategory_shouldThrowWhenParentTypeDoesNotMatch() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category category = new Category(
                user,
                "Salary",
                CategoryType.INCOME,
                null,
                false
        );

        Category parent = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(categoryRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        when(transactionRepository.existsByCategory_Id(category.getId()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                categoryService.updateCategory(
                        user.getId(),
                        category.getId(),
                        new UpdateCategoryRequest(
                                "Salary",
                                parent.getId()
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Category type must match parent category type"
                );
    }

    @Test
    void updateCategory_shouldThrowWhenCategoryIsItsOwnParent() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                false
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(transactionRepository.existsByCategory_Id(category.getId()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                categoryService.updateCategory(
                        user.getId(),
                        category.getId(),
                        new UpdateCategoryRequest(
                                "Food",
                                category.getId()
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A category cannot be its own parent");
    }

    @Test
    void updateCategory_shouldThrowWhenMovedUnderDescendant() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category parent = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                false
        );

        Category child = new Category(
                user,
                "Restaurants",
                CategoryType.EXPENSE,
                parent,
                false
        );

        when(categoryRepository.findById(parent.getId()))
                .thenReturn(Optional.of(parent));

        when(categoryRepository.findById(child.getId()))
                .thenReturn(Optional.of(child));

        when(categoryRepository.findAllByParent_Id(child.getId()))
                .thenReturn(List.of());

        when(transactionRepository.existsByCategory_Id(parent.getId()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                categoryService.updateCategory(
                        user.getId(),
                        parent.getId(),
                        new UpdateCategoryRequest(
                                "Food",
                                child.getId()
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "A category cannot be moved under one of its descendants"
                );
    }

    @Test
    void deleteCategory_shouldDeleteCustomCategory() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category category = new Category(
                user,
                "Custom",
                CategoryType.EXPENSE,
                null,
                false
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByParent_Id(category.getId()))
                .thenReturn(false);

        when(transactionRepository.existsByCategory_Id(category.getId()))
                .thenReturn(false);

        categoryService.deleteCategory(
                user.getId(),
                category.getId()
        );

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_shouldThrowWhenDefaultCategory() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() ->
                categoryService.deleteCategory(
                        user.getId(),
                        category.getId()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Default categories cannot be deleted");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_shouldThrowWhenCategoryHasChildren() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                false
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByParent_Id(category.getId()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                categoryService.deleteCategory(
                        user.getId(),
                        category.getId()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Category with child categories cannot be deleted"
                );

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_shouldThrowWhenCategoryHasTransactions() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category category = new Category(
                user,
                "Custom",
                CategoryType.EXPENSE,
                null,
                false
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByParent_Id(category.getId()))
                .thenReturn(false);

        when(transactionRepository.existsByCategory_Id(category.getId()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                categoryService.deleteCategory(
                        user.getId(),
                        category.getId()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Category with transaction history cannot be deleted"
                );

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_shouldThrowWhenCategoryBelongsToAnotherUser() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);

        CategoryService categoryService =
                new CategoryService(
                        categoryRepository,
                        userRepository,
                        transactionRepository
                );

        User owner = new User(
                "owner@example.com",
                "hashed-password",
                "Owner"
        );

        User anotherUser = new User(
                "other@example.com",
                "hashed-password",
                "Other"
        );

        Category category = new Category(
                owner,
                "Custom",
                CategoryType.EXPENSE,
                null,
                false
        );

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() ->
                categoryService.deleteCategory(
                        anotherUser.getId(),
                        category.getId()
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
