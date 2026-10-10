import { apiRequest } from "./apiClient";
import type {
  Budget,
  CreateBudgetRequest,
  UpdateBudgetRequest,
} from "../types/budget";

const BUDGET_API_URL = "http://localhost:8080/api/budgets";

export async function getBudgets(
  year: number,
  month: number,
): Promise<Budget[]> {
  return apiRequest<Budget[]>(`${BUDGET_API_URL}?year=${year}&month=${month}`);
}

export async function createBudget(
  year: number,
  month: number,
  request: CreateBudgetRequest,
): Promise<Budget> {
  return apiRequest<Budget>(`${BUDGET_API_URL}?year=${year}&month=${month}`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function updateBudget(
  budgetId: string,
  request: UpdateBudgetRequest,
): Promise<Budget> {
  return apiRequest<Budget>(`${BUDGET_API_URL}/${budgetId}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export async function deleteBudget(budgetId: string): Promise<void> {
  await apiRequest<void>(`${BUDGET_API_URL}/${budgetId}`, {
    method: "DELETE",
  });
}
