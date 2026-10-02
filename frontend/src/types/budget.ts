export type BudgetPeriod = "MONTHLY" | "WEEKLY" | "YEARLY";

export type Budget = {
  id: string;
  categoryId: string;
  categoryName: string;
  amount: number;
  period: BudgetPeriod;
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateBudgetRequest = {
  categoryId: string;
  amount: number;
  period: BudgetPeriod;
  startDate: string;
  endDate: string;
};
