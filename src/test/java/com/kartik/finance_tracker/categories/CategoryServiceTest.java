package com.kartik.finance_tracker.categories;

import static org.assertj.core.api.Assertions.assertThat;
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
}
