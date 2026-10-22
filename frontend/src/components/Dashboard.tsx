import { useEffect, useMemo, useState } from "react";
import { ArrowDownRight, ArrowUpRight, PiggyBank, Percent } from "lucide-react";

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
    return { year: year - 1, month: 12 };
  }

  return { year, month: month - 1 };
}

function getNextMonth(year: number, month: number) {
  if (month === 12) {
    return { year: year + 1, month: 1 };
  }

  return { year, month: month + 1 };
}

function getMonthLabel(year: number, month: number) {
  return new Date(year, month - 1, 1).toLocaleDateString(undefined, {
    month: "long",
    year: "numeric",
  });
}

function formatAmount(amount: number) {
  return amount.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatCompactAmount(amount: number) {
  return new Intl.NumberFormat("en-IN", {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(amount);
}

function formatCurrency(amount: number) {
  return `₹${formatAmount(amount)}`;
}

const CATEGORY_COLORS = [
  "#16a34a",
  "#3b82f6",
  "#f59e0b",
  "#8b5cf6",
  "#ec4899",
  "#06b6d4",
  "#f97316",
  "#64748b",
];

function getCategoryColor(categoryId: string) {
  let hash = 0;

  for (let index = 0; index < categoryId.length; index++) {
    hash = (hash * 31 + categoryId.charCodeAt(index)) >>> 0;
  }

  return CATEGORY_COLORS[hash % CATEGORY_COLORS.length];
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
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      setIsLoading(true);
      setError(null);

      try {
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

        if (cancelled) {
          return;
        }

        setMonthlySummary(monthlySummaryResponse);
        setSpendingByCategory(spendingByCategoryResponse);
        setIncomeVsExpense(incomeVsExpenseResponse);
        setAccountBalances(accountBalancesResponse);
        setExpandedCategories(new Set());
      } catch (err) {
        if (cancelled) {
          return;
        }

        setError(
          err instanceof Error ? err.message : "Failed to load dashboard data.",
        );
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadDashboard();

    return () => {
      cancelled = true;
    };
  }, [selectedYear, selectedMonth, refreshKey, retryKey]);

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

  const totalCategorySpending = categorySpendingGroups.reduce(
    (total, group) => total + group.amount,
    0,
  );

  const visibleAccountBalances = accountBalances.filter(
    (account) => account.accountType !== "GOAL",
  );

  const categoryDonutBackground = useMemo(() => {
    const positiveGroups = categorySpendingGroups.filter(
      (group) => group.amount > 0,
    );

    if (totalCategorySpending <= 0 || positiveGroups.length === 0) {
      return "conic-gradient(#e5e7eb 0deg 360deg)";
    }

    let currentPercentage = 0;

    const segments = positiveGroups.map((group) => {
      const start = currentPercentage;
      currentPercentage += (group.amount / totalCategorySpending) * 100;

      return `${getCategoryColor(group.category.id)} ${
        start * 3.6
      }deg ${currentPercentage * 3.6}deg`;
    });

    return `conic-gradient(${segments.join(", ")})`;
  }, [categorySpendingGroups, totalCategorySpending]);

  const maxMonthlyAmount = Math.max(
    ...incomeVsExpense.flatMap((entry) => [entry.income, entry.expenses]),
    0,
  );

  const chartMax = maxMonthlyAmount > 0 ? maxMonthlyAmount * 1.1 : 1;

  const chartMonths = Array.from({ length: 12 }, (_, index) => {
    const month = index + 1;
    const entry = incomeVsExpense.find((item) => item.month === month);

    return {
      month,
      income: entry?.income ?? 0,
      expenses: entry?.expenses ?? 0,
    };
  });

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
    return (
      <section className="dashboard-page" aria-busy="true">
        <h2>Dashboard</h2>
        <p role="status">Loading dashboard...</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className="dashboard-page">
        <h2>Dashboard</h2>

        <div className="dashboard-error" role="alert">
          <p>Could not load dashboard data. Please try again.</p>

          <button
            type="button"
            onClick={() => setRetryKey((current) => current + 1)}
          >
            Retry loading dashboard
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="dashboard-page">
      <header className="dashboard-header">
        <p className="dashboard-subtitle">An overview of your finances.</p>
      </header>

      <section className="dashboard-month-section">
        <div className="dashboard-month-navigation">
          <button
            type="button"
            className="dashboard-month-button"
            onClick={handlePreviousMonth}
            aria-label="View previous month"
          >
            <span aria-hidden="true">←</span>
          </button>

          <h3>{getMonthLabel(selectedYear, selectedMonth)}</h3>

          <button
            type="button"
            className="dashboard-month-button"
            onClick={handleNextMonth}
            disabled={isCurrentMonth()}
            aria-label="View next month"
          >
            <span aria-hidden="true">→</span>
          </button>
        </div>
      </section>

      {monthlySummary && (
        <section className="dashboard-summary-section">
          <div className="dashboard-section-heading">
            <div>
              <h3>Monthly Overview</h3>
              <p>Your financial summary for the selected month.</p>
            </div>
          </div>

          <div className="dashboard-summary-grid">
            <article className="dashboard-summary-card income">
              <div className="dashboard-summary-icon" aria-hidden="true">
                <ArrowUpRight size={20} strokeWidth={2} />
              </div>

              <div className="dashboard-summary-content">
                <p className="dashboard-summary-label">Total Income</p>
                <p className="dashboard-summary-value">
                  {formatCurrency(monthlySummary.totalIncome)}
                </p>
              </div>
            </article>

            <article className="dashboard-summary-card expenses">
              <div className="dashboard-summary-icon" aria-hidden="true">
                <ArrowDownRight size={20} strokeWidth={2} />
              </div>

              <div className="dashboard-summary-content">
                <p className="dashboard-summary-label">Total Expenses</p>
                <p className="dashboard-summary-value">
                  {formatCurrency(monthlySummary.totalExpenses)}
                </p>
              </div>
            </article>

            <article className="dashboard-summary-card savings">
              <div className="dashboard-summary-icon" aria-hidden="true">
                <PiggyBank size={20} strokeWidth={2} />
              </div>

              <div className="dashboard-summary-content">
                <p className="dashboard-summary-label">Net Savings</p>
                <p className="dashboard-summary-value">
                  {formatCurrency(monthlySummary.netSavings)}
                </p>
              </div>
            </article>

            <article className="dashboard-summary-card rate">
              <div className="dashboard-summary-icon" aria-hidden="true">
                <Percent size={20} strokeWidth={2} />
              </div>

              <div className="dashboard-summary-content">
                <p className="dashboard-summary-label">Savings Rate</p>
                <p className="dashboard-summary-value">
                  {formatAmount(monthlySummary.savingsRate)}%
                </p>
              </div>
            </article>
          </div>
        </section>
      )}

      <div className="dashboard-content-grid">
        <section className="dashboard-panel dashboard-accounts-panel">
          <div className="dashboard-section-heading">
            <div>
              <h3>Account Balances</h3>
              <p>Balances across your accounts.</p>
            </div>
          </div>

          {visibleAccountBalances.length === 0 ? (
            <p className="dashboard-empty-state">No accounts to display yet.</p>
          ) : (
            <ul className="dashboard-account-list">
              {visibleAccountBalances.map((account) => (
                <li key={account.accountId}>
                  <div className="dashboard-account-details">
                    <span className="dashboard-account-name">
                      {account.accountName}
                    </span>

                    <span className="dashboard-account-type">
                      {account.accountType.replaceAll("_", " ")}
                    </span>

                    {account.accountType === "CREDIT_CARD" && (
                      <span className="dashboard-account-credit">
                        Available credit:{" "}
                        {formatCurrency(account.availableCredit ?? 0)}
                        {account.creditLimit !== null && (
                          <> of {formatCurrency(account.creditLimit)}</>
                        )}
                      </span>
                    )}
                  </div>

                  <strong
                    className={`dashboard-account-balance ${
                      account.accountType === "CREDIT_CARD"
                        ? "credit-card-debt"
                        : ""
                    }`}
                  >
                    {account.accountType === "CREDIT_CARD"
                      ? account.balance > 0
                        ? "Owed "
                        : account.balance < 0
                          ? "Credit "
                          : ""
                      : ""}
                    {formatCurrency(Math.abs(account.balance))}

                    {account.accountType === "CREDIT_CARD" &&
                      account.balance < 0 && (
                        <span className="dashboard-account-credit-note">
                          {" "}
                          (credit balance)
                        </span>
                      )}
                  </strong>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="dashboard-panel dashboard-category-panel">
          <div className="dashboard-section-heading">
            <div>
              <h3>Spending by Category</h3>
              <p>Where your money went this month.</p>
            </div>
          </div>

          {categorySpendingGroups.length === 0 ? (
            <p className="dashboard-empty-state">
              No spending recorded this month.
            </p>
          ) : (
            <div className="dashboard-category-visualization">
              <div className="dashboard-donut-wrapper">
                <div
                  className="dashboard-donut-chart"
                  style={{ background: categoryDonutBackground }}
                  role="img"
                  aria-label={`Spending breakdown totaling ${formatCurrency(
                    totalCategorySpending,
                  )}`}
                >
                  <div className="dashboard-donut-center">
                    <span>Total spent</span>
                    <strong>{formatCurrency(totalCategorySpending)}</strong>
                  </div>
                </div>
              </div>

              <div className="dashboard-category-list">
                {categorySpendingGroups.map((group) => {
                  const isExpanded = expandedCategories.has(group.category.id);
                  const hasChildren = group.children.length > 0;

                  const percentage =
                    totalCategorySpending > 0
                      ? (group.amount / totalCategorySpending) * 100
                      : 0;

                  return (
                    <div
                      className="dashboard-category-group"
                      key={group.category.id}
                    >
                      <div className="dashboard-category-row">
                        <div className="dashboard-category-name">
                          {hasChildren ? (
                            <button
                              type="button"
                              className="dashboard-category-toggle"
                              onClick={() => toggleCategory(group.category.id)}
                              aria-expanded={isExpanded}
                              aria-label={`Toggle ${group.category.name}`}
                            >
                              {isExpanded ? "▾" : "▸"}
                            </button>
                          ) : (
                            <span
                              className="dashboard-category-bullet"
                              style={{
                                color: getCategoryColor(group.category.id),
                              }}
                              aria-hidden="true"
                            >
                              ●
                            </span>
                          )}

                          <strong>{group.category.name}</strong>
                        </div>

                        <div className="dashboard-category-amount-group">
                          <strong className="dashboard-category-amount">
                            {formatCurrency(group.amount)}
                          </strong>

                          <span className="dashboard-category-percentage">
                            {percentage.toFixed(1)}%
                          </span>
                        </div>
                      </div>

                      {isExpanded && hasChildren && (
                        <ul className="dashboard-category-children">
                          {group.children
                            .slice()
                            .sort((a, b) => b.amount - a.amount)
                            .map((child) => (
                              <li key={child.categoryId}>
                                <span>{child.categoryName}</span>
                                <span>{formatCurrency(child.amount)}</span>
                              </li>
                            ))}
                        </ul>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </section>
      </div>

      <section className="dashboard-panel dashboard-income-expense-panel">
        <div className="dashboard-section-heading">
          <div>
            <h3>Income vs. Expenses</h3>
            <p>Your monthly income and expenses for {selectedYear}.</p>
          </div>
        </div>

        {incomeVsExpense.length === 0 ? (
          <p className="dashboard-empty-state">
            No income or expense data found for this year.
          </p>
        ) : (
          <>
            <div className="dashboard-chart-legend">
              <span>
                <span
                  className="dashboard-legend-dot income-dot"
                  aria-hidden="true"
                />
                Income
              </span>

              <span>
                <span
                  className="dashboard-legend-dot expense-dot"
                  aria-hidden="true"
                />
                Expenses
              </span>
            </div>

            <div className="dashboard-chart-scroll">
              <svg
                className="dashboard-income-expense-chart"
                viewBox="0 0 720 280"
                role="img"
                aria-label={`Monthly income and expenses for ${selectedYear}`}
              >
                {[0, 1, 2, 3, 4].map((step) => {
                  const y = 220 - step * 50;

                  return (
                    <g key={step}>
                      <line
                        x1="48"
                        y1={y}
                        x2="710"
                        y2={y}
                        className="dashboard-chart-gridline"
                      />

                      <text
                        x="40"
                        y={y + 4}
                        textAnchor="end"
                        className="dashboard-chart-axis-label"
                      >
                        {formatCompactAmount((chartMax * step) / 4)}
                      </text>
                    </g>
                  );
                })}

                {chartMonths.map((entry, index) => {
                  const groupX = 57 + index * 55;
                  const incomeHeight = (entry.income / chartMax) * 200;
                  const expenseHeight = (entry.expenses / chartMax) * 200;

                  const monthLabel = new Date(
                    selectedYear,
                    entry.month - 1,
                    1,
                  ).toLocaleDateString(undefined, {
                    month: "short",
                  });

                  return (
                    <g key={entry.month}>
                      <rect
                        x={groupX}
                        y={220 - incomeHeight}
                        width="17"
                        height={incomeHeight}
                        rx="3"
                        className="dashboard-chart-income-bar"
                      >
                        <title>
                          {monthLabel} income: {formatCurrency(entry.income)}
                        </title>
                      </rect>

                      <rect
                        x={groupX + 20}
                        y={220 - expenseHeight}
                        width="17"
                        height={expenseHeight}
                        rx="3"
                        className="dashboard-chart-expense-bar"
                      >
                        <title>
                          {monthLabel} expenses:{" "}
                          {formatCurrency(entry.expenses)}
                        </title>
                      </rect>

                      <text
                        x={groupX + 18}
                        y="246"
                        textAnchor="middle"
                        className="dashboard-chart-month-label"
                      >
                        {monthLabel}
                      </text>
                    </g>
                  );
                })}
              </svg>
            </div>
          </>
        )}
      </section>
    </section>
  );
}
