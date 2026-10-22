import { apiRequest } from "./apiClient";
import type { AccountType } from "../types/account";

export type AccountBalance = {
  accountId: string;
  accountName: string;
  accountType: AccountType;
  currency: string;
  balance: number;
  creditLimit: number | null;
  availableCredit: number | null;
};

export type MonthlySummary = {
  year: number;
  month: number;
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  savingsRate: number;
};

export type SpendingByCategory = {
  categoryId: string;
  categoryName: string;
  amount: number;
};

export type IncomeVsExpense = {
  month: number;
  income: number;
  expenses: number;
};

export async function getMonthlySummary(
  year: number,
  month: number,
): Promise<MonthlySummary> {
  return apiRequest<MonthlySummary>(
    `http://localhost:8080/api/analytics/summary?year=${year}&month=${month}`,
  );
}

export async function getSpendingByCategory(
  year: number,
  month: number,
): Promise<SpendingByCategory[]> {
  return apiRequest<SpendingByCategory[]>(
    `http://localhost:8080/api/analytics/spending-by-category?year=${year}&month=${month}`,
  );
}

export async function getIncomeVsExpense(
  year: number,
): Promise<IncomeVsExpense[]> {
  return apiRequest<IncomeVsExpense[]>(
    `http://localhost:8080/api/analytics/income-vs-expense?year=${year}`,
  );
}

export async function getAccountBalances(): Promise<AccountBalance[]> {
  return apiRequest<AccountBalance[]>(
    "http://localhost:8080/api/analytics/account-balances",
  );
}
