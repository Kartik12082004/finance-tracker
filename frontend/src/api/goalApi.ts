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
  sourceAccountId: string,
  amount: number,
): Promise<Goal> {
  return apiRequest<Goal>(
    `http://localhost:8080/api/goals/${goalId}/contributions`,
    {
      method: "POST",
      body: JSON.stringify({
        sourceAccountId,
        amount,
      }),
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

export async function deleteGoal(goalId: string): Promise<void> {
  await apiRequest<void>(`http://localhost:8080/api/goals/${goalId}`, {
    method: "DELETE",
  });
}

export async function getArchivedGoals(): Promise<Goal[]> {
  return apiRequest<Goal[]>("http://localhost:8080/api/goals/archived");
}

export async function archiveGoal(goalId: string): Promise<Goal> {
  return apiRequest<Goal>(`http://localhost:8080/api/goals/${goalId}/archive`, {
    method: "PATCH",
  });
}

export async function unarchiveGoal(goalId: string): Promise<Goal> {
  return apiRequest<Goal>(
    `http://localhost:8080/api/goals/${goalId}/unarchive`,
    {
      method: "PATCH",
    },
  );
}
