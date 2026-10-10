import { useEffect, useState } from "react";

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
  return amount.toFixed(2);
}

function getMonthLabel(year: number, month: number): string {
  return new Date(year, month - 1, 1).toLocaleDateString("en-US", {
    month: "long",
    year: "numeric",
  });
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

export default function Budgets({ refreshKey = 0 }: BudgetsProps) {
  const now = new Date();

  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);

  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [categoryId, setCategoryId] = useState("");
  const [amount, setAmount] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  const [editingBudgetId, setEditingBudgetId] = useState<string | null>(null);
  const [editAmount, setEditAmount] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);

  const [deletingBudgetId, setDeletingBudgetId] = useState<string | null>(null);

  const isCurrentMonth =
    selectedYear === now.getFullYear() && selectedMonth === now.getMonth() + 1;

  useEffect(() => {
    async function loadBudgets() {
      try {
        setIsLoading(true);
        setError(null);

        const [budgetsResponse, categoriesResponse] = await Promise.all([
          getBudgets(selectedYear, selectedMonth),
          getCategories(),
        ]);

        setBudgets(budgetsResponse);
        setCategories(categoriesResponse);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load budgets.",
        );
      } finally {
        setIsLoading(false);
      }
    }

    void loadBudgets();
  }, [selectedYear, selectedMonth, refreshKey]);

  async function refreshBudgets() {
    const updatedBudgets = await getBudgets(selectedYear, selectedMonth);
    setBudgets(updatedBudgets);
  }

  function goToPreviousMonth() {
    const previous = getPreviousMonth(selectedYear, selectedMonth);

    setSelectedYear(previous.year);
    setSelectedMonth(previous.month);
    setEditingBudgetId(null);
    setEditAmount("");
    setError(null);
  }

  function goToNextMonth() {
    if (isCurrentMonth) {
      return;
    }

    const next = getNextMonth(selectedYear, selectedMonth);

    setSelectedYear(next.year);
    setSelectedMonth(next.month);
    setEditingBudgetId(null);
    setEditAmount("");
    setError(null);
  }

  async function handleCreateBudget(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const parsedAmount = Number(amount);

    if (!categoryId) {
      setError("Category is required.");
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

  function cancelEditingBudget() {
    setEditingBudgetId(null);
    setEditAmount("");
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

      cancelEditingBudget();
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

    if (!confirmed) {
      return;
    }

    try {
      setDeletingBudgetId(budget.id);
      setError(null);

      await deleteBudget(budget.id);

      setBudgets((currentBudgets) =>
        currentBudgets.filter(
          (currentBudget) => currentBudget.id !== budget.id,
        ),
      );

      if (editingBudgetId === budget.id) {
        cancelEditingBudget();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete budget.");
    } finally {
      setDeletingBudgetId(null);
    }
  }

  const expenseCategories = categories.filter(
    (category) => category.type === "EXPENSE" && category.parentId === null,
  );

  if (isLoading) {
    return (
      <section>
        <h2>Budgets</h2>
        <p>Loading budgets...</p>
      </section>
    );
  }

  return (
    <section>
      <div>
        <h2>Budgets</h2>

        <div>
          <button type="button" onClick={goToPreviousMonth}>
            Previous
          </button>

          <strong>{getMonthLabel(selectedYear, selectedMonth)}</strong>

          <button
            type="button"
            onClick={goToNextMonth}
            disabled={isCurrentMonth}
          >
            Next
          </button>
        </div>
      </div>

      {error && <p>{error}</p>}

      <section>
        <h3>Create Budget</h3>

        <form onSubmit={handleCreateBudget}>
          <div>
            <label htmlFor="budget-category">Category</label>

            <select
              id="budget-category"
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
            >
              <option value="">Select category</option>

              {expenseCategories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="budget-amount">Monthly amount</label>

            <input
              id="budget-amount"
              type="number"
              min="0.01"
              step="0.01"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
            />
          </div>

          <button type="submit" disabled={isCreating}>
            {isCreating ? "Creating..." : "Create Budget"}
          </button>
        </form>
      </section>

      <section>
        <h3>{getMonthLabel(selectedYear, selectedMonth)} Budgets</h3>

        {budgets.length === 0 ? (
          <p>No budgets found for this month.</p>
        ) : (
          <ul>
            {budgets.map((budget) => (
              <li key={budget.id}>
                <article>
                  <h4>{budget.categoryName}</h4>

                  {editingBudgetId === budget.id ? (
                    <form
                      onSubmit={(event) => handleUpdateBudget(event, budget.id)}
                    >
                      <div>
                        <p>
                          Category: <strong>{budget.categoryName}</strong>
                        </p>

                        <p>
                          Month:{" "}
                          <strong>
                            {getMonthLabel(budget.year, budget.month)}
                          </strong>
                        </p>
                      </div>

                      <div>
                        <label htmlFor={`edit-budget-amount-${budget.id}`}>
                          Amount
                        </label>

                        <input
                          id={`edit-budget-amount-${budget.id}`}
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={editAmount}
                          onChange={(event) =>
                            setEditAmount(event.target.value)
                          }
                        />
                      </div>

                      <button type="submit" disabled={isUpdating}>
                        {isUpdating ? "Saving..." : "Save Changes"}
                      </button>

                      <button
                        type="button"
                        onClick={cancelEditingBudget}
                        disabled={isUpdating}
                      >
                        Cancel
                      </button>
                    </form>
                  ) : (
                    <>
                      <p>Budget: ₹{formatAmount(budget.amount)}</p>

                      <p>Spent: ₹{formatAmount(budget.spentAmount)}</p>

                      <p>Remaining: ₹{formatAmount(budget.remainingAmount)}</p>

                      <p>Used: {formatAmount(budget.spendingPercentage)}%</p>

                      <button
                        type="button"
                        onClick={() => startEditingBudget(budget)}
                      >
                        Edit
                      </button>

                      <button
                        type="button"
                        onClick={() => void handleDeleteBudget(budget)}
                        disabled={deletingBudgetId === budget.id}
                      >
                        {deletingBudgetId === budget.id
                          ? "Deleting..."
                          : "Delete"}
                      </button>
                    </>
                  )}
                </article>
              </li>
            ))}
          </ul>
        )}
      </section>
    </section>
  );
}
