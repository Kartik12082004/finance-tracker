import { useEffect, useState } from "react";
import {
  createBudget,
  deleteBudget,
  getBudgets,
  updateBudget,
} from "../api/budgetApi";
import type {
  Budget,
  BudgetPeriod,
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

function formatDate(date: string): string {
  return new Date(`${date}T00:00:00`).toLocaleDateString("en-GB");
}

export default function Budgets({ refreshKey = 0 }: BudgetsProps) {
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [categoryId, setCategoryId] = useState("");
  const [amount, setAmount] = useState("");
  const [period, setPeriod] = useState<BudgetPeriod>("MONTHLY");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const [isCreating, setIsCreating] = useState(false);

  const [editingBudgetId, setEditingBudgetId] = useState<string | null>(null);
  const [editAmount, setEditAmount] = useState("");
  const [editPeriod, setEditPeriod] = useState<BudgetPeriod>("MONTHLY");
  const [editStartDate, setEditStartDate] = useState("");
  const [editEndDate, setEditEndDate] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);
  const [deletingBudgetId, setDeletingBudgetId] = useState<string | null>(null);

  useEffect(() => {
    async function loadBudgets() {
      try {
        setIsLoading(true);
        setError(null);

        const [budgetsResponse, categoriesResponse] = await Promise.all([
          getBudgets(),
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
  }, [refreshKey]);

  async function refreshBudgets() {
    const updatedBudgets = await getBudgets();
    setBudgets(updatedBudgets);
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

    if (!startDate) {
      setError("Start date is required.");
      return;
    }

    if (!endDate) {
      setError("End date is required.");
      return;
    }

    if (endDate < startDate) {
      setError("End date cannot be before start date.");
      return;
    }

    const request: CreateBudgetRequest = {
      categoryId,
      amount: parsedAmount,
      period,
      startDate,
      endDate,
    };

    try {
      setIsCreating(true);
      setError(null);

      await createBudget(request);

      setCategoryId("");
      setAmount("");
      setPeriod("MONTHLY");
      setStartDate("");
      setEndDate("");

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
    setEditPeriod(budget.period);
    setEditStartDate(budget.startDate);
    setEditEndDate(budget.endDate);
    setError(null);
  }

  function cancelEditingBudget() {
    setEditingBudgetId(null);
    setEditAmount("");
    setEditPeriod("MONTHLY");
    setEditStartDate("");
    setEditEndDate("");
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

    if (!editStartDate) {
      setError("Start date is required.");
      return;
    }

    if (!editEndDate) {
      setError("End date is required.");
      return;
    }

    if (editEndDate < editStartDate) {
      setError("End date cannot be before start date.");
      return;
    }

    const request: UpdateBudgetRequest = {
      amount: parsedAmount,
      period: editPeriod,
      startDate: editStartDate,
      endDate: editEndDate,
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
      `Are you sure you want to permanently delete the ${budget.categoryName} budget? This cannot be undone.`,
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
    (category) => category.type === "EXPENSE",
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
      <h2>Budgets</h2>

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
            <label htmlFor="budget-amount">Amount</label>

            <input
              id="budget-amount"
              type="number"
              min="0.01"
              step="0.01"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="budget-period">Period</label>

            <select
              id="budget-period"
              value={period}
              onChange={(event) =>
                setPeriod(event.target.value as BudgetPeriod)
              }
            >
              <option value="MONTHLY">Monthly</option>
              <option value="WEEKLY">Weekly</option>
              <option value="YEARLY">Yearly</option>
            </select>
          </div>

          <div>
            <label htmlFor="budget-start-date">Start date</label>

            <input
              id="budget-start-date"
              type="date"
              value={startDate}
              onChange={(event) => setStartDate(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="budget-end-date">End date</label>

            <input
              id="budget-end-date"
              type="date"
              value={endDate}
              onChange={(event) => setEndDate(event.target.value)}
            />
          </div>

          <button type="submit" disabled={isCreating}>
            {isCreating ? "Creating..." : "Create Budget"}
          </button>
        </form>
      </section>

      <section>
        <h3>Your Budgets</h3>

        {budgets.length === 0 ? (
          <p>No budgets found.</p>
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
                        <p>Category cannot be changed while editing.</p>
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

                      <div>
                        <label htmlFor={`edit-budget-period-${budget.id}`}>
                          Period
                        </label>

                        <select
                          id={`edit-budget-period-${budget.id}`}
                          value={editPeriod}
                          onChange={(event) =>
                            setEditPeriod(event.target.value as BudgetPeriod)
                          }
                        >
                          <option value="MONTHLY">Monthly</option>
                          <option value="WEEKLY">Weekly</option>
                          <option value="YEARLY">Yearly</option>
                        </select>
                      </div>

                      <div>
                        <label htmlFor={`edit-budget-start-${budget.id}`}>
                          Start date
                        </label>

                        <input
                          id={`edit-budget-start-${budget.id}`}
                          type="date"
                          value={editStartDate}
                          onChange={(event) =>
                            setEditStartDate(event.target.value)
                          }
                        />
                      </div>

                      <div>
                        <label htmlFor={`edit-budget-end-${budget.id}`}>
                          End date
                        </label>

                        <input
                          id={`edit-budget-end-${budget.id}`}
                          type="date"
                          value={editEndDate}
                          onChange={(event) =>
                            setEditEndDate(event.target.value)
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

                      <p>Period: {budget.period}</p>

                      <p>Start: {formatDate(budget.startDate)}</p>

                      <p>End: {formatDate(budget.endDate)}</p>

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
