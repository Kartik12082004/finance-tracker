import { useEffect, useMemo, useState } from "react";
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
import type { Category } from "../types/category";

type DashboardProps = {
  refreshKey: number;
  categories: Category[];
};

type CategorySpendingGroup = {
  category: Category;
  amount: number;
  children: SpendingByCategory[];
};

function getInitialMonth() {
  const now = new Date();

  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1,
  };
}

function getPreviousMonth(year: number, month: number) {
  if (month === 1) {
    return {
      year: year - 1,
      month: 12,
    };
  }

  return {
    year,
    month: month - 1,
  };
}

function getNextMonth(year: number, month: number) {
  if (month === 12) {
    return {
      year: year + 1,
      month: 1,
    };
  }

  return {
    year,
    month: month + 1,
  };
}

function getMonthLabel(year: number, month: number) {
  return new Date(year, month - 1, 1).toLocaleDateString(undefined, {
    month: "long",
    year: "numeric",
  });
}

function formatAmount(amount: number) {
  return amount.toFixed(2);
}

export default function Dashboard({ refreshKey, categories }: DashboardProps) {
  const initialMonth = getInitialMonth();

  const [selectedYear, setSelectedYear] = useState(initialMonth.year);

  const [selectedMonth, setSelectedMonth] = useState(initialMonth.month);

  const [monthlySummary, setMonthlySummary] = useState<MonthlySummary | null>(
    null,
  );

  const [spendingByCategory, setSpendingByCategory] = useState<
    SpendingByCategory[]
  >([]);

  const [incomeVsExpense, setIncomeVsExpense] = useState<IncomeVsExpense[]>([]);

  const [accountBalances, setAccountBalances] = useState<AccountBalance[]>([]);

  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(
    new Set(),
  );

  const [isLoading, setIsLoading] = useState(true);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadDashboard() {
      try {
        setIsLoading(true);
        setError(null);

        const [
          monthlySummaryResponse,
          spendingByCategoryResponse,
          incomeVsExpenseResponse,
          accountBalancesResponse,
        ] = await Promise.all([
          getMonthlySummary(selectedYear, selectedMonth),
          getSpendingByCategory(selectedYear, selectedMonth),
          getIncomeVsExpense(selectedYear),
          getAccountBalances(),
        ]);

        setMonthlySummary(monthlySummaryResponse);
        setSpendingByCategory(spendingByCategoryResponse);
        setIncomeVsExpense(incomeVsExpenseResponse);
        setAccountBalances(accountBalancesResponse);
        setExpandedCategories(new Set());
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load dashboard data.",
        );
      } finally {
        setIsLoading(false);
      }
    }

    void loadDashboard();
  }, [selectedYear, selectedMonth, refreshKey]);

  const categorySpendingGroups = useMemo<CategorySpendingGroup[]>(() => {
    const categoryMap = new Map(
      categories.map((category) => [category.id, category]),
    );

    const grouped = new Map<
      string,
      {
        category: Category;
        amount: number;
        children: SpendingByCategory[];
      }
    >();

    for (const spending of spendingByCategory) {
      const category = categoryMap.get(spending.categoryId);

      if (!category) {
        continue;
      }

      const parentCategory = category.parentId
        ? categoryMap.get(category.parentId)
        : null;

      const groupCategory = parentCategory ?? category;

      if (!grouped.has(groupCategory.id)) {
        grouped.set(groupCategory.id, {
          category: groupCategory,
          amount: 0,
          children: [],
        });
      }

      const group = grouped.get(groupCategory.id)!;

      group.amount += spending.amount;

      if (parentCategory) {
        group.children.push(spending);
      }
    }

    return Array.from(grouped.values()).sort((a, b) => b.amount - a.amount);
  }, [categories, spendingByCategory]);

  function toggleCategory(categoryId: string) {
    setExpandedCategories((current) => {
      const next = new Set(current);

      if (next.has(categoryId)) {
        next.delete(categoryId);
      } else {
        next.add(categoryId);
      }

      return next;
    });
  }

  function handlePreviousMonth() {
    const previous = getPreviousMonth(selectedYear, selectedMonth);

    setSelectedYear(previous.year);
    setSelectedMonth(previous.month);
  }

  function handleNextMonth() {
    const next = getNextMonth(selectedYear, selectedMonth);

    setSelectedYear(next.year);
    setSelectedMonth(next.month);
  }

  function isCurrentMonth() {
    const now = new Date();

    return (
      selectedYear === now.getFullYear() && selectedMonth === now.getMonth() + 1
    );
  }

  if (isLoading) {
    return <p>Loading dashboard...</p>;
  }

  if (error) {
    return <p>{error}</p>;
  }

  return (
    <section>
      <h2>Dashboard</h2>

      <section>
        <div>
          <button type="button" onClick={handlePreviousMonth}>
            ←
          </button>

          <strong>{getMonthLabel(selectedYear, selectedMonth)}</strong>

          <button
            type="button"
            onClick={handleNextMonth}
            disabled={isCurrentMonth()}
          >
            →
          </button>
        </div>
      </section>

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

        {categorySpendingGroups.length === 0 ? (
          <p>No spending recorded this month.</p>
        ) : (
          <div>
            {categorySpendingGroups.map((group) => {
              const isExpanded = expandedCategories.has(group.category.id);

              const hasChildren = group.children.length > 0;

              return (
                <div key={group.category.id}>
                  <div>
                    {hasChildren ? (
                      <button
                        type="button"
                        onClick={() => toggleCategory(group.category.id)}
                        aria-expanded={isExpanded}
                        aria-label={`Toggle ${group.category.name}`}
                      >
                        {isExpanded ? "▾" : "▸"}
                      </button>
                    ) : (
                      <span>•</span>
                    )}

                    <strong>{group.category.name}</strong>

                    <span> {formatAmount(group.amount)}</span>
                  </div>

                  {isExpanded && hasChildren && (
                    <ul>
                      {group.children
                        .slice()
                        .sort((a, b) => b.amount - a.amount)
                        .map((child) => (
                          <li key={child.categoryId}>
                            {child.categoryName}: {formatAmount(child.amount)}
                          </li>
                        ))}
                    </ul>
                  )}
                </div>
              );
            })}
          </div>
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
