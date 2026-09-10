package com.kartik.finance_tracker.categories;

import java.util.UUID;

import org.springframework.stereotype.Service;

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

    public Category createCategory(
            UUID userId,
            String name,
            CategoryType type,
            UUID parentId,
            boolean isDefault
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Category parent = null;

        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));

            if (!parent.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Parent category does not belong to user");
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

}
