import { useEffect, useState } from "react";
import {
  getAccountBalances,
  getIncomeVsExpense,
  getMonthlySummary,
  getSpendingByCategory,
  type AccountBalance,
  type IncomeVsExpense,
  type MonthlySummary,
  type SpendingByCategory,
} from "../api/analyticsApi";

type DashboardProps = {
  refreshKey: number;
};

export default function Dashboard({ refreshKey }: DashboardProps) {
  const [monthlySummary, setMonthlySummary] = useState<MonthlySummary | null>(
    null,
  );

  const [spendingByCategory, setSpendingByCategory] = useState<
    SpendingByCategory[]
  >([]);

  const [incomeVsExpense, setIncomeVsExpense] = useState<IncomeVsExpense[]>([]);

  const [accountBalances, setAccountBalances] = useState<AccountBalance[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadDashboard() {
      try {
        setIsLoading(true);
        setError(null);

        const now = new Date();
        const year = now.getFullYear();
        const month = now.getMonth() + 1;

        const [
          monthlySummaryResponse,
          spendingByCategoryResponse,
          incomeVsExpenseResponse,
          accountBalancesResponse,
        ] = await Promise.all([
          getMonthlySummary(year, month),
          getSpendingByCategory(year, month),
          getIncomeVsExpense(year),
          getAccountBalances(),
        ]);

        setMonthlySummary(monthlySummaryResponse);
        setSpendingByCategory(spendingByCategoryResponse);
        setIncomeVsExpense(incomeVsExpenseResponse);
        setAccountBalances(accountBalancesResponse);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load dashboard data.",
        );
      } finally {
        setIsLoading(false);
      }
    }

    void loadDashboard();
  }, [refreshKey]);

  if (isLoading) {
    return <p>Loading dashboard...</p>;
  }

  if (error) {
    return <p>{error}</p>;
  }

  return (
    <section>
      <h2>Dashboard</h2>

      {monthlySummary && (
        <section>
          <h3>
            Monthly Summary — {monthlySummary.year}-{monthlySummary.month}
          </h3>

          <p>Total income: {monthlySummary.totalIncome}</p>
          <p>Total expenses: {monthlySummary.totalExpenses}</p>
          <p>Net savings: {monthlySummary.netSavings}</p>
          <p>Savings rate: {monthlySummary.savingsRate}%</p>
        </section>
      )}

      <section>
        <h3>Account Balances</h3>

        {accountBalances.length === 0 ? (
          <p>No accounts found.</p>
        ) : (
          <ul>
            {accountBalances.map((account) => (
              <li key={account.accountId}>
                {account.accountName}: {account.balance}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h3>Spending by Category</h3>

        {spendingByCategory.length === 0 ? (
          <p>No spending recorded this month.</p>
        ) : (
          <ul>
            {spendingByCategory.map((category) => (
              <li key={category.categoryId}>
                {category.categoryName}: {category.amount}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h3>Income vs Expense</h3>

        {incomeVsExpense.length === 0 ? (
          <p>No income or expense data found for this year.</p>
        ) : (
          <ul>
            {incomeVsExpense.map((entry) => (
              <li key={entry.month}>
                Month {entry.month} — Income: {entry.income}, Expenses:{" "}
                {entry.expenses}
              </li>
            ))}
          </ul>
        )}
      </section>
    </section>
  );
}
