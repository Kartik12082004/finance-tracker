package com.kartik.finance_tracker.categories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

public class CategoryServiceTest {
    
    @Test 
    void createCategory_shouldSaveAndReturnCategory() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        CategoryService categoryService =
                new CategoryService(categoryRepository, userRepository);

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

        CategoryService categoryService =
                new CategoryService(categoryRepository, userRepository);

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
                true
        );

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        Category result = categoryService.createCategory(
                userId,
                "Restaurants",
                CategoryType.EXPENSE,
                parent.getId(),
                true
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
    void createCategory_shouldThrowWhenUserDoesNotExist() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        CategoryService categoryService =
                new CategoryService(categoryRepository, userRepository);

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void createCategory_shouldThrowWhenParentDoesNotExist() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        CategoryService categoryService =
                new CategoryService(categoryRepository, userRepository);

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
                        true
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent category not found");
    }

    @Test
    void createCategory_shouldThrowWhenParentBelongsToAnotherUser() {
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        UserRepository userRepository =
                mock(UserRepository.class);

        CategoryService categoryService =
                new CategoryService(categoryRepository, userRepository);

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent category does not belong to user");
    }

}
