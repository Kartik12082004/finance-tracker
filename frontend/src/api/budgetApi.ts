import { apiRequest } from "./apiClient";
import type { Budget, CreateBudgetRequest } from "../types/budget";

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
