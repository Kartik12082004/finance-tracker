import { apiRequest } from "./apiClient";
import type { CreateGoalRequest, Goal, UpdateGoalRequest } from "../types/goal";

export async function getGoals(): Promise<Goal[]> {
  return apiRequest<Goal[]>("http://localhost:8080/api/goals");
}

export async function createGoal(request: CreateGoalRequest): Promise<Goal> {
  return apiRequest<Goal>("http://localhost:8080/api/goals", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function updateGoal(
  goalId: string,
  request: UpdateGoalRequest,
): Promise<Goal> {
  return apiRequest<Goal>(`http://localhost:8080/api/goals/${goalId}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export async function contributeToGoal(
  goalId: string,
  amount: number,
): Promise<Goal> {
  return apiRequest<Goal>(
    `http://localhost:8080/api/goals/${goalId}/contributions?amount=${amount}`,
    {
      method: "POST",
    },
  );
}

export async function pauseGoal(goalId: string): Promise<Goal> {
  return apiRequest<Goal>(`http://localhost:8080/api/goals/${goalId}/pause`, {
    method: "PATCH",
  });
}

export async function resumeGoal(goalId: string): Promise<Goal> {
  return apiRequest<Goal>(`http://localhost:8080/api/goals/${goalId}/resume`, {
    method: "PATCH",
  });
}

export async function cancelGoal(goalId: string): Promise<Goal> {
  return apiRequest<Goal>(`http://localhost:8080/api/goals/${goalId}/cancel`, {
    method: "PATCH",
  });
}
