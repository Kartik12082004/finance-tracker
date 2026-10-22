import { useEffect, useMemo, useState } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Pencil,
  Trash2,
  Wallet,
  TrendingDown,
  CircleDollarSign,
  Plus,
  X,
} from "lucide-react";
import {
  createBudget,
  deleteBudget,
  getBudgets,
  updateBudget,
} from "../api/budgetApi";
import type {
  Budget,
  CreateBudgetRequest,
  UpdateBudgetRequest,
} from "../types/budget";
import { getCategories } from "../api/categoryApi";
import type { Category } from "../types/category";

type BudgetsProps = {
  refreshKey?: number;
};

function formatAmount(amount: number): string {
  return amount.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function getMonthLabel(year: number, month: number): string {
  return new Date(year, month - 1, 1).toLocaleDateString("en-US", {
    month: "long",
    year: "numeric",
  });
}

function getPreviousMonth(year: number, month: number) {
  return month === 1
    ? { year: year - 1, month: 12 }
    : { year, month: month - 1 };
}

function getNextMonth(year: number, month: number) {
  return month === 12
    ? { year: year + 1, month: 1 }
    : { year, month: month + 1 };
}

function getProgressState(percentage: number) {
  if (percentage >= 100) return "over";
  if (percentage >= 80) return "warning";
  return "normal";
}

export default function Budgets({ refreshKey = 0 }: BudgetsProps) {
  const now = new Date();

  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  const [categoryId, setCategoryId] = useState("");
  const [amount, setAmount] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  const [editingBudgetId, setEditingBudgetId] = useState<string | null>(null);
  const [editAmount, setEditAmount] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);
  const [deletingBudgetId, setDeletingBudgetId] = useState<string | null>(null);

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const isCurrentMonth =
    selectedYear === now.getFullYear() && selectedMonth === now.getMonth() + 1;

  useEffect(() => {
    let cancelled = false;

    async function loadBudgets() {
      setIsLoading(true);
      setLoadError(null);

      try {
        const [budgetsResponse, categoriesResponse] = await Promise.all([
          getBudgets(selectedYear, selectedMonth),
          getCategories(),
        ]);

        if (cancelled) return;

        setBudgets(budgetsResponse);
        setCategories(categoriesResponse);
      } catch {
        if (cancelled) return;

        setLoadError(
          "Could not load budgets. Check your connection and try again.",
        );
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    void loadBudgets();

    return () => {
      cancelled = true;
    };
  }, [selectedYear, selectedMonth, refreshKey, retryKey]);

  async function refreshBudgets() {
    try {
      const updatedBudgets = await getBudgets(selectedYear, selectedMonth);
      setBudgets(updatedBudgets);
    } catch {
      setError(
        "Your budget change was saved, but the latest budgets could not be loaded. Use Retry to refresh the displayed data.",
      );
      setLoadError(
        "Your budget change was saved, but the latest budgets could not be loaded. Retry to reload.",
      );
    }
  }

  function clearEditing() {
    setEditingBudgetId(null);
    setEditAmount("");
  }

  function goToPreviousMonth() {
    const previous = getPreviousMonth(selectedYear, selectedMonth);
    setSelectedYear(previous.year);
    setSelectedMonth(previous.month);
    clearEditing();
    setError(null);
  }

  function goToNextMonth() {
    if (isCurrentMonth) return;

    const next = getNextMonth(selectedYear, selectedMonth);
    setSelectedYear(next.year);
    setSelectedMonth(next.month);
    clearEditing();
    setError(null);
  }

  async function handleCreateBudget(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const parsedAmount = Number(amount);

    if (!categoryId) {
      setError("Choose a category for this budget.");
      return;
    }

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError("Budget amount must be greater than zero.");
      return;
    }

    if (!Number.isInteger(parsedAmount * 100)) {
      setError("Budget amount cannot have more than 2 decimal places.");
      return;
    }

    const request: CreateBudgetRequest = {
      categoryId,
      amount: parsedAmount,
    };

    try {
      setIsCreating(true);
      setError(null);

      await createBudget(selectedYear, selectedMonth, request);
      setIsCreateModalOpen(false);
      setCategoryId("");
      setAmount("");
      await refreshBudgets();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create budget.");
    } finally {
      setIsCreating(false);
    }
  }

  function startEditingBudget(budget: Budget) {
    setEditingBudgetId(budget.id);
    setEditAmount(String(budget.amount));
    setError(null);
  }

  async function handleUpdateBudget(
    event: React.FormEvent<HTMLFormElement>,
    budgetId: string,
  ) {
    event.preventDefault();

    const parsedAmount = Number(editAmount);

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError("Budget amount must be greater than zero.");
      return;
    }

    if (!Number.isInteger(parsedAmount * 100)) {
      setError("Budget amount cannot have more than 2 decimal places.");
      return;
    }

    const request: UpdateBudgetRequest = {
      amount: parsedAmount,
    };

    try {
      setIsUpdating(true);
      setError(null);

      await updateBudget(budgetId, request);
      await refreshBudgets();
      clearEditing();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update budget.");
    } finally {
      setIsUpdating(false);
    }
  }

  async function handleDeleteBudget(budget: Budget) {
    const confirmed = window.confirm(
      `Are you sure you want to permanently delete the ${budget.categoryName} budget for ${getMonthLabel(
        budget.year,
        budget.month,
      )}?`,
    );

    if (!confirmed) return;

    try {
      setDeletingBudgetId(budget.id);
      setError(null);

      await deleteBudget(budget.id);

      setBudgets((current) => current.filter((item) => item.id !== budget.id));

      if (editingBudgetId === budget.id) clearEditing();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete budget.");
    } finally {
      setDeletingBudgetId(null);
    }
  }

  const expenseCategories = useMemo(
    () =>
      categories.filter(
        (category) => category.type === "EXPENSE" && category.parentId === null,
      ),
    [categories],
  );

  const totalBudget = budgets.reduce((sum, budget) => sum + budget.amount, 0);
  const totalSpent = budgets.reduce(
    (sum, budget) => sum + budget.spentAmount,
    0,
  );
  const remaining = totalBudget - totalSpent;
  const overallPercentage =
    totalBudget > 0 ? (totalSpent / totalBudget) * 100 : 0;

  const currency = (value: number) => `₹${formatAmount(value)}`;

  if (isLoading) {
    return (
      <div className="budgets-page">
        <div className="budgets-loading" role="status">
          <span className="budgets-loading-spinner" />
          <p>Loading your budgets...</p>
        </div>
      </div>
    );
  }

  if (loadError) {
    return (
      <section className="budgets-page">
        <header className="budgets-header">
          <div>
            <p className="eyebrow">Plan with purpose</p>
            <h2>Budgets</h2>
            <p className="budgets-subtitle">
              Keep your spending on track, one month at a time.
            </p>
          </div>
        </header>
        <div className="budgets-error" role="alert">
          <p>{loadError}</p>
          <button
            type="button"
            className="secondary-button"
            onClick={() => setRetryKey((current) => current + 1)}
            disabled={isLoading}
          >
            Retry
          </button>
        </div>
      </section>
    );
  }

  return (
    <div className="budgets-page">
      <header className="budgets-header">
        <div>
          <p className="eyebrow">Plan with purpose</p>
          <h2>Budgets</h2>
          <p className="budgets-subtitle">
            Keep your spending on track, one month at a time.
          </p>
        </div>

        <div className="budgets-header-actions">
          <div className="budgets-month-navigation">
            <button
              type="button"
              className="budgets-month-button"
              onClick={goToPreviousMonth}
              aria-label="Previous month"
            >
              <ChevronLeft size={18} />
            </button>

            <strong>{getMonthLabel(selectedYear, selectedMonth)}</strong>

            <button
              type="button"
              className="budgets-month-button"
              onClick={goToNextMonth}
              disabled={isCurrentMonth}
              aria-label="Next month"
            >
              <ChevronRight size={18} />
            </button>
          </div>

          <button
            type="button"
            className="primary-button budgets-add-button"
            onClick={() => {
              setError(null);
              setCategoryId("");
              setAmount("");
              setIsCreateModalOpen(true);
            }}
          >
            <Plus size={17} />
            Add budget
          </button>
        </div>
      </header>

      {loadError && (
        <div className="budgets-error" role="alert">
          <p>{loadError}</p>
          <button
            type="button"
            className="secondary-button"
            onClick={() => setRetryKey((current) => current + 1)}
            disabled={isLoading}
          >
            Retry
          </button>
        </div>
      )}

      {error && (
        <div className="budgets-error" role="alert">
          {error}
        </div>
      )}

      <section className="budgets-summary-grid" aria-label="Budget overview">
        <article className="budgets-summary-card">
          <div className="budgets-summary-icon">
            <Wallet size={20} />
          </div>
          <div>
            <p>Total budget</p>
            <strong>{currency(totalBudget)}</strong>
            <span>Planned for this month</span>
          </div>
        </article>

        <article className="budgets-summary-card">
          <div className="budgets-summary-icon spent">
            <TrendingDown size={20} />
          </div>
          <div>
            <p>Total spent</p>
            <strong>{currency(totalSpent)}</strong>
            <span>
              {totalBudget > 0
                ? `${formatAmount(overallPercentage)}% of budget used`
                : "No budget allocated yet"}
            </span>
          </div>
        </article>

        <article className="budgets-summary-card">
          <div className="budgets-summary-icon remaining">
            <CircleDollarSign size={20} />
          </div>
          <div>
            <p>Remaining</p>
            <strong className={remaining < 0 ? "is-over-budget" : ""}>
              {currency(remaining)}
            </strong>
            <span>
              {remaining < 0
                ? "Above your planned budget"
                : "Available within your budgets"}
            </span>
          </div>
        </article>
      </section>

      {isCreateModalOpen && (
        <div
          className="budget-modal-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isCreating) {
              setIsCreateModalOpen(false);
              setError(null);
            }
          }}
        >
          <section
            className="budget-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="budget-modal-title"
          >
            <header className="budget-modal-header">
              <div>
                <p className="eyebrow">Set a limit</p>
                <h2 id="budget-modal-title">Add budget</h2>
                <p>
                  Set a monthly spending limit for{" "}
                  {getMonthLabel(selectedYear, selectedMonth)}.
                </p>
              </div>

              <button
                type="button"
                className="budget-modal-close"
                aria-label="Close dialog"
                onClick={() => {
                  setIsCreateModalOpen(false);
                  setError(null);
                }}
                disabled={isCreating}
              >
                <X size={20} />
              </button>
            </header>

            <form className="budgets-create-form" onSubmit={handleCreateBudget}>
              <label>
                <span>Expense category</span>
                <select
                  value={categoryId}
                  onChange={(event) => setCategoryId(event.target.value)}
                  disabled={isCreating}
                  required
                >
                  <option value="">Select a category</option>
                  {expenseCategories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Monthly limit (₹)</span>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  inputMode="decimal"
                  placeholder="e.g. 5000"
                  value={amount}
                  onChange={(event) => setAmount(event.target.value)}
                  disabled={isCreating}
                  required
                />
              </label>

              {expenseCategories.length === 0 && (
                <p className="budgets-field-hint">
                  Create an expense category first to set a budget.
                </p>
              )}

              {error && (
                <div className="budgets-error" role="alert">
                  {error}
                </div>
              )}

              <div className="budget-modal-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
                    setIsCreateModalOpen(false);
                    setError(null);
                  }}
                  disabled={isCreating}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={isCreating || expenseCategories.length === 0}
                >
                  {isCreating ? "Creating..." : "Create budget"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      <section className="budgets-list-section">
        <div className="budgets-list-heading">
          <div>
            <p className="eyebrow">Your spending limits</p>
            <h2>Monthly budgets</h2>
          </div>
          <span className="budgets-count">
            {budgets.length} {budgets.length === 1 ? "budget" : "budgets"}
          </span>
        </div>

        {budgets.length === 0 ? (
          <div className="budgets-empty-state">
            <div className="budgets-empty-icon">
              <Wallet size={24} />
            </div>
            <h3>No budgets for this month</h3>
            <p>
              Create a budget above to start tracking spending in{" "}
              {getMonthLabel(selectedYear, selectedMonth)}.
            </p>
          </div>
        ) : (
          <div className="budgets-card-grid">
            {budgets.map((budget) => {
              const percentage = Number.isFinite(budget.spendingPercentage)
                ? budget.spendingPercentage
                : 0;
              const progressState = getProgressState(percentage);
              const progressWidth = Math.min(100, Math.max(0, percentage));

              return (
                <article className="budget-card" key={budget.id}>
                  <div className="budget-card-header">
                    <div className="budget-category-icon">
                      <Wallet size={19} />
                    </div>

                    <div className="budget-card-title">
                      <h3>{budget.categoryName}</h3>
                      <span>{getMonthLabel(budget.year, budget.month)}</span>
                    </div>

                    <div className="budget-card-actions">
                      <button
                        type="button"
                        className="budget-icon-button"
                        onClick={() => startEditingBudget(budget)}
                        aria-label={`Edit ${budget.categoryName} budget`}
                        title="Edit budget"
                        disabled={editingBudgetId === budget.id}
                      >
                        <Pencil size={16} />
                      </button>
                      <button
                        type="button"
                        className="budget-icon-button delete"
                        onClick={() => void handleDeleteBudget(budget)}
                        aria-label={`Delete ${budget.categoryName} budget`}
                        title="Delete budget"
                        disabled={deletingBudgetId === budget.id}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>

                  {editingBudgetId === budget.id ? (
                    <form
                      className="budget-edit-form"
                      onSubmit={(event) =>
                        void handleUpdateBudget(event, budget.id)
                      }
                    >
                      <label>
                        <span>Monthly limit (₹)</span>
                        <input
                          type="number"
                          min="0.01"
                          step="0.01"
                          inputMode="decimal"
                          value={editAmount}
                          onChange={(event) =>
                            setEditAmount(event.target.value)
                          }
                          disabled={isUpdating}
                          required
                        />
                      </label>

                      <div className="budget-edit-actions">
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={clearEditing}
                          disabled={isUpdating}
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          className="primary-button"
                          disabled={isUpdating}
                        >
                          {isUpdating ? "Saving..." : "Save changes"}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <>
                      <div className="budget-amount-row">
                        <div>
                          <p>Spent so far</p>
                          <strong>{currency(budget.spentAmount)}</strong>
                        </div>
                        <div className="budget-limit-amount">
                          <p>Budget limit</p>
                          <strong>{currency(budget.amount)}</strong>
                        </div>
                      </div>

                      <div className="budget-progress-heading">
                        <span>Budget used</span>
                        <strong
                          className={`budget-percentage ${progressState}`}
                        >
                          {formatAmount(percentage)}%
                        </strong>
                      </div>

                      <div
                        className="budget-progress-track"
                        role="progressbar"
                        aria-label={`${budget.categoryName} budget used`}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-valuenow={Math.min(100, Math.max(0, percentage))}
                      >
                        <div
                          className={`budget-progress-fill ${progressState}`}
                          style={{ width: `${progressWidth}%` }}
                        />
                      </div>

                      <div className="budget-card-footer">
                        <span>
                          {budget.remainingAmount < 0
                            ? "Over budget by"
                            : "Remaining"}
                        </span>
                        <strong
                          className={
                            budget.remainingAmount < 0 ? "is-over-budget" : ""
                          }
                        >
                          {currency(Math.abs(budget.remainingAmount))}
                        </strong>
                      </div>

                      {percentage >= 100 && (
                        <p className="budget-status over">
                          Budget limit reached or exceeded.
                        </p>
                      )}

                      {percentage >= 80 && percentage < 100 && (
                        <p className="budget-status warning">
                          You're getting close to this budget's limit.
                        </p>
                      )}
                    </>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
