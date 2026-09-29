package com.kartik.finance_tracker.categories;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Category createCategory(
            UUID userId,
            String name,
            CategoryType type,
            UUID parentId,
            boolean isDefault
    ) {
        // A category must always belong to an existing user.
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Category parent = null;

        if (parentId != null) {
            // Find the requested parent before creating the category.
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Parent category not found"
                            ));

            // Categories are user-owned, so a user cannot use another
            // user's category as the parent of their own category.
            if (!parent.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException(
                        "Parent category not found"
                );
            }

            // A child category must have the same type as its parent.
            if (parent.getType() != type) {
                throw new IllegalArgumentException(
                        "Category type must match parent category type"
                );
            }
        }

        Category category = new Category(
                user,
                name,
                type,
                parent,
                isDefault
        );

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories(UUID userId) {
        // Only return categories owned by the requested user.
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return categoryRepository.findAllByUser_Id(userId);
    }
}
