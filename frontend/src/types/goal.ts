export type GoalStatus = "ACTIVE" | "COMPLETED" | "PAUSED" | "CANCELLED";

export type Goal = {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  status: GoalStatus;
  createdAt: string;
  updatedAt: string;
};

export type CreateGoalRequest = {
  name: string;
  targetAmount: number;
  targetDate: string;
};

export type UpdateGoalRequest = {
  name: string;
  targetAmount: number;
  targetDate: string;
};
