package com.kartik.finance_tracker.categories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kartik.finance_tracker.users.User;

public class CategoryControllerTest {

    @Test
    void createCategory_shouldReturnCreatedCategory() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);

        CategoryController categoryController =
                new CategoryController(categoryService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();

        UUID userId = UUID.randomUUID();

        Category category = new Category(
                new User(
                        "kartik@example.com",
                        "hashed-password",
                        "Kartik"
                ),
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        when(categoryService.createCategory(
                any(UUID.class),
                any(String.class),
                any(CategoryType.class),
                any(),
                any(Boolean.class)
        )).thenReturn(category);

        // The API should return 201 when a category is successfully created.
        mockMvc.perform(post("/api/categories")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Food",
                            "type": "EXPENSE",
                            "isDefault": true
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Food"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void createCategory_shouldReturnCreatedChildCategory() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);

        CategoryController categoryController =
                new CategoryController(categoryService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();

        UUID userId = UUID.randomUUID();

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
                true
        );

        Category child = new Category(
                user,
                "Groceries",
                CategoryType.EXPENSE,
                parent,
                false
        );

        // The API should return the parent's ID when creating a child category.
        when(categoryService.createCategory(
                any(UUID.class),
                any(String.class),
                any(CategoryType.class),
                any(),
                any(Boolean.class)
        )).thenReturn(child);

        mockMvc.perform(post("/api/categories")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Groceries",
                            "type": "EXPENSE",
                            "parentId": "%s",
                            "isDefault": false
                        }
                        """.formatted(parent.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.parentId").value(parent.getId().toString()))
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    void createCategory_shouldReturnBadRequestWhenNameIsMissing() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);
        CategoryController categoryController = new CategoryController(categoryService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        UUID userId = UUID.randomUUID();

        // The API must reject a request without a category name.
        mockMvc.perform(post("/api/categories")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "EXPENSE"
                        }
                        """))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_shouldReturnBadRequestWhenTypeIsMissing() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);
        CategoryController categoryController = new CategoryController(categoryService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        UUID userId = UUID.randomUUID();

        // The API must reject a request without a category type.
        mockMvc.perform(post("/api/categories")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Food"
                        }
                        """))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_shouldReturnBadRequestWhenNameIsTooLong() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);
        CategoryController categoryController = new CategoryController(categoryService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        UUID userId = UUID.randomUUID();

        String longName = "a".repeat(101);

        // Category names are limited to 100 characters.
        mockMvc.perform(post("/api/categories")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "%s",
                            "type": "EXPENSE"
                        }
                        """.formatted(longName)))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(categoryService);
    }

    @Test
    void getCategories_shouldReturnUserCategories() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);

        CategoryController categoryController =
                new CategoryController(categoryService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();

        UUID userId = UUID.randomUUID();

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Category foodCategory = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        Category groceriesCategory = new Category(
                user,
                "Groceries",
                CategoryType.EXPENSE,
                foodCategory,
                false
        );

        when(categoryService.getCategories(userId))
                .thenReturn(List.of(foodCategory, groceriesCategory));

        // The API should return all categories belonging to the requested user.
        mockMvc.perform(get("/api/categories")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].name").value("Groceries"))
                .andExpect(jsonPath("$[1].type").value("EXPENSE"))
                .andExpect(jsonPath("$[1].parentId").value(foodCategory.getId().toString()))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    @Test
    void getCategories_shouldReturnEmptyListWhenUserHasNoCategories() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);

        CategoryController categoryController =
                new CategoryController(categoryService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();

        UUID userId = UUID.randomUUID();

        when(categoryService.getCategories(userId))
                .thenReturn(List.of());

        // A user with no categories should receive an empty array rather than null.
        mockMvc.perform(get("/api/categories")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}