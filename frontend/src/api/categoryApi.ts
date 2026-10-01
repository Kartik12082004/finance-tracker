import { apiRequest } from "./apiClient";
import type { Category } from "../types/category";

export async function getCategories(): Promise<Category[]> {
  return apiRequest<Category[]>("http://localhost:8080/api/categories");
}
