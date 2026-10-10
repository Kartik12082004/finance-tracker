import { apiRequest } from "./apiClient";
import type { Category, CategoryType } from "../types/category";

const CATEGORY_API_URL = "http://localhost:8080/api/categories";

export type CreateCategoryRequest = {
  name: string;
  type: CategoryType;
  parentId: string | null;
};

export type UpdateCategoryRequest = {
  name: string;
  parentId: string | null;
};

export async function getCategories(): Promise<Category[]> {
  return apiRequest<Category[]>(CATEGORY_API_URL);
}

export async function createCategory(
  request: CreateCategoryRequest,
): Promise<Category> {
  return apiRequest<Category>(CATEGORY_API_URL, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function updateCategory(
  categoryId: string,
  request: UpdateCategoryRequest,
): Promise<Category> {
  return apiRequest<Category>(`${CATEGORY_API_URL}/${categoryId}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export async function deleteCategory(categoryId: string): Promise<void> {
  await apiRequest<void>(`${CATEGORY_API_URL}/${categoryId}`, {
    method: "DELETE",
  });
}
