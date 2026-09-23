package com.kartik.finance_tracker.categories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kartik.finance_tracker.security.CurrentUserService;
import com.kartik.finance_tracker.users.User;

public class CategoryControllerTest {

    private CategoryService categoryService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);
        currentUserService = mock(CurrentUserService.class);

        CategoryController categoryController =
                new CategoryController(categoryService, currentUserService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();
    }

    @Test
    void createCategory_shouldReturnCreatedCategory() throws Exception {
        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

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

        // isDefault is controlled by the backend and is not accepted from clients.
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Food",
                            "type": "EXPENSE"
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
        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

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

        // The backend decides whether a category is a default category.
        when(categoryService.createCategory(
                any(UUID.class),
                any(String.class),
                any(CategoryType.class),
                any(),
                any(Boolean.class)
        )).thenReturn(child);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Groceries",
                            "type": "EXPENSE",
                            "parentId": "%s"
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
        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        // The API must reject a request without a category name.
        mockMvc.perform(post("/api/categories")
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
        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        // The API must reject a request without a category type.
        mockMvc.perform(post("/api/categories")
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
        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        String longName = "a".repeat(101);

        // Category names are limited to 100 characters.
        mockMvc.perform(post("/api/categories")
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
        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

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

        // The API should return all categories belonging to the authenticated user.
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].name").value("Groceries"))
                .andExpect(jsonPath("$[1].type").value("EXPENSE"))
                .andExpect(jsonPath("$[1].parentId")
                        .value(foodCategory.getId().toString()))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    @Test
    void getCategories_shouldReturnEmptyListWhenUserHasNoCategories()
            throws Exception {
        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(categoryService.getCategories(userId))
                .thenReturn(List.of());

        // A user with no categories should receive an empty array rather than null.
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
