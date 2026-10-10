export type Budget = {
  id: string;
  categoryId: string;
  categoryName: string;
  amount: number;
  spentAmount: number;
  remainingAmount: number;
  spendingPercentage: number;
  year: number;
  month: number;
  createdAt: string;
  updatedAt: string;
};

export type CreateBudgetRequest = {
  categoryId: string;
  amount: number;
};

export type UpdateBudgetRequest = {
  amount: number;
};
