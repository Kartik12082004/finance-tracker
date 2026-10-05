import { apiRequest } from "./apiClient";
import type {
  Budget,
  CreateBudgetRequest,
  UpdateBudgetRequest,
} from "../types/budget";

export async function getBudgets(): Promise<Budget[]> {
  return apiRequest<Budget[]>("http://localhost:8080/api/budgets");
}

export async function getBudgetsForDate(date: string): Promise<Budget[]> {
  return apiRequest<Budget[]>(`http://localhost:8080/api/budgets?date=${date}`);
}

export async function createBudget(
  request: CreateBudgetRequest,
): Promise<Budget> {
  return apiRequest<Budget>("http://localhost:8080/api/budgets", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function updateBudget(
  budgetId: string,
  request: UpdateBudgetRequest,
): Promise<Budget> {
  return apiRequest<Budget>(`http://localhost:8080/api/budgets/${budgetId}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export async function deleteBudget(budgetId: string): Promise<void> {
  await apiRequest<void>(`http://localhost:8080/api/budgets/${budgetId}`, {
    method: "DELETE",
  });
}
