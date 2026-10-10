import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import {
  createRecurringTransaction,
  deleteRecurringTransaction,
  getRecurringTransactions,
  skipNextRecurringTransaction,
  undoSkipNextRecurringTransaction,
  updateRecurringTransaction,
} from "../api/recurringTransactionApi";

import { getAccounts } from "../api/accountApi";
import { getCategories } from "../api/categoryApi";

import type { Account } from "../types/account";
import type { Category } from "../types/category";
import type {
  CreateRecurringTransactionRequest,
  RecurringTransaction,
  UpdateRecurringTransactionRequest,
} from "../types/recurringTransaction";
import type { TransactionType } from "../types/transaction";

function formatAmount(amount: number): string {
  return amount.toFixed(2);
}

function formatDate(date: string): string {
  return new Date(`${date}T00:00:00`).toLocaleDateString("en-GB");
}

function formatFrequency(
  unit: RecurringTransaction["frequencyUnit"],
  interval: number,
): string {
  if (unit === "MONTH" && interval === 1) {
    return "Monthly";
  }

  const unitLabel =
    interval === 1 ? unit.toLowerCase() : `${unit.toLowerCase()}s`;

  return `Every ${interval} ${unitLabel}`;
}

function getToday(): string {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

export default function RecurringTransactions() {
  const [recurringTransactions, setRecurringTransactions] = useState<
    RecurringTransaction[]
  >([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [paymentDate, setPaymentDate] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadData() {
      try {
        setIsLoading(true);
        setError(null);

        const [
          recurringTransactionsResponse,
          accountsResponse,
          categoriesResponse,
        ] = await Promise.all([
          getRecurringTransactions(),
          getAccounts(),
          getCategories(),
        ]);

        if (cancelled) {
          return;
        }

        setRecurringTransactions(recurringTransactionsResponse);
        setAccounts(accountsResponse);
        setCategories(categoriesResponse);
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : "Failed to load recurring transactions.",
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadData();

    return () => {
      cancelled = true;
    };
  }, []);

  function resetForm() {
    setEditingId(null);
    setAccountId("");
    setCategoryId("");
    setType("EXPENSE");
    setAmount("");
    setDescription("");
    setPaymentDate("");
  }

  function handleEdit(recurringTransaction: RecurringTransaction) {
    setEditingId(recurringTransaction.id);
    setAccountId(recurringTransaction.accountId);
    setCategoryId(recurringTransaction.categoryId);
    setType(recurringTransaction.type);
    setAmount(String(recurringTransaction.amount));
    setDescription(recurringTransaction.description ?? "");
    setPaymentDate(recurringTransaction.nextOccurrence);
    setError(null);

    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const parsedAmount = Number(amount);

    if (!accountId) {
      setError("Account is required.");
      return;
    }

    if (!categoryId) {
      setError("Category is required.");
      return;
    }

    if (
      !Number.isFinite(parsedAmount) ||
      parsedAmount <= 0 ||
      !/^\d+(\.\d{1,2})?$/.test(amount.trim())
    ) {
      setError("Amount must be positive and have at most two decimal places.");
      return;
    }

    if (!paymentDate) {
      setError("First payment date is required.");
      return;
    }

    if (paymentDate < getToday()) {
      setError("Payment date cannot be in the past.");
      return;
    }

    const request: CreateRecurringTransactionRequest = {
      accountId,
      categoryId,
      type,
      amount: parsedAmount,
      description: description.trim(),
      frequencyUnit: "MONTH",
      frequencyInterval: 1,
      nextOccurrence: paymentDate,
    };

    try {
      setIsSaving(true);
      setError(null);

      if (editingId) {
        const updateRequest: UpdateRecurringTransactionRequest = request;

        const updatedRecurringTransaction = await updateRecurringTransaction(
          editingId,
          updateRequest,
        );

        setRecurringTransactions((current) =>
          current.map((transaction) =>
            transaction.id === editingId
              ? updatedRecurringTransaction
              : transaction,
          ),
        );
      } else {
        const createdRecurringTransaction =
          await createRecurringTransaction(request);

        setRecurringTransactions((current) => [
          ...current,
          createdRecurringTransaction,
        ]);
      }

      resetForm();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : editingId
            ? "Failed to update recurring transaction."
            : "Failed to create recurring transaction.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  async function handleSkipNext(recurringTransactionId: string) {
    try {
      setProcessingId(recurringTransactionId);
      setError(null);

      const updatedRecurringTransaction = await skipNextRecurringTransaction(
        recurringTransactionId,
      );

      setRecurringTransactions((current) =>
        current.map((transaction) =>
          transaction.id === recurringTransactionId
            ? updatedRecurringTransaction
            : transaction,
        ),
      );
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to skip the next recurring transaction.",
      );
    } finally {
      setProcessingId(null);
    }
  }

  async function handleUndoSkip(recurringTransactionId: string) {
    try {
      setProcessingId(recurringTransactionId);
      setError(null);

      const updatedRecurringTransaction =
        await undoSkipNextRecurringTransaction(recurringTransactionId);

      setRecurringTransactions((current) =>
        current.map((transaction) =>
          transaction.id === recurringTransactionId
            ? updatedRecurringTransaction
            : transaction,
        ),
      );
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to undo the scheduled skip.",
      );
    } finally {
      setProcessingId(null);
    }
  }

  async function handleDelete(recurringTransactionId: string) {
    const confirmed = window.confirm(
      "Delete this recurring transaction rule? Existing transactions will not be affected.",
    );

    if (!confirmed) {
      return;
    }

    try {
      setProcessingId(recurringTransactionId);
      setError(null);

      await deleteRecurringTransaction(recurringTransactionId);

      setRecurringTransactions((current) =>
        current.filter(
          (transaction) => transaction.id !== recurringTransactionId,
        ),
      );

      if (editingId === recurringTransactionId) {
        resetForm();
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to delete recurring transaction.",
      );
    } finally {
      setProcessingId(null);
    }
  }

  const availableAccounts = accounts.filter(
    (account) =>
      account.type !== "GOAL" ||
      account.id ===
        recurringTransactions.find(
          (transaction) => transaction.id === editingId,
        )?.accountId,
  );

  const availableCategories = categories.filter(
    (category) => category.type === type,
  );

  if (isLoading) {
    return (
      <section>
        <h2>Recurring Transactions</h2>
        <p>Loading recurring transactions...</p>
      </section>
    );
  }

  return (
    <section>
      <h2>Recurring Transactions</h2>

      {error && <p role="alert">{error}</p>}

      <section>
        <h3>
          {editingId ? "Edit Recurring Payment" : "Create Recurring Payment"}
        </h3>

        <p>
          Set a monthly payment and choose its first payment date. The recurring
          schedule will use that calendar day for future months.
        </p>

        <form onSubmit={handleSave}>
          <div>
            <label htmlFor="recurring-account">Account</label>
            <select
              id="recurring-account"
              value={accountId}
              onChange={(event) => setAccountId(event.target.value)}
              required
            >
              <option value="">Select account</option>
              {availableAccounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="recurring-type">Type</label>
            <select
              id="recurring-type"
              value={type}
              onChange={(event) => {
                const nextType = event.target.value as TransactionType;
                setType(nextType);
                setCategoryId("");
              }}
              required
            >
              <option value="EXPENSE">Expense</option>
              <option value="INCOME">Income</option>
            </select>
          </div>

          <div>
            <label htmlFor="recurring-category">Category</label>
            <select
              id="recurring-category"
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
              required
            >
              <option value="">Select category</option>
              {availableCategories.map((category: Category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="recurring-amount">Amount</label>
            <input
              id="recurring-amount"
              type="number"
              min="0.01"
              step="0.01"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              required
            />
          </div>

          <div>
            <label htmlFor="recurring-description">Description</label>
            <input
              id="recurring-description"
              type="text"
              maxLength={255}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="recurring-payment-date">
              {editingId ? "Next payment date" : "First payment date"}
            </label>
            <input
              id="recurring-payment-date"
              type="date"
              min={getToday()}
              value={paymentDate}
              onChange={(event) => setPaymentDate(event.target.value)}
              required
            />
          </div>

          <p>Frequency: Monthly</p>

          <button type="submit" disabled={isSaving}>
            {isSaving
              ? "Saving..."
              : editingId
                ? "Save Changes"
                : "Create Recurring Payment"}
          </button>

          {editingId && (
            <button type="button" onClick={resetForm} disabled={isSaving}>
              Cancel Edit
            </button>
          )}
        </form>
      </section>

      <section>
        <h3>Your Recurring Payments</h3>

        {recurringTransactions.length === 0 ? (
          <p>No recurring payments found.</p>
        ) : (
          <ul>
            {recurringTransactions.map((recurringTransaction) => {
              const isProcessing = processingId === recurringTransaction.id;

              return (
                <li key={recurringTransaction.id}>
                  <article>
                    <h4>
                      {recurringTransaction.description?.trim() ||
                        recurringTransaction.categoryName}
                    </h4>

                    <p>Type: {recurringTransaction.type}</p>
                    <p>Amount: ₹{formatAmount(recurringTransaction.amount)}</p>
                    <p>Category: {recurringTransaction.categoryName}</p>

                    <p>
                      Account:{" "}
                      {accounts.find(
                        (account) =>
                          account.id === recurringTransaction.accountId,
                      )?.name ?? recurringTransaction.accountId}
                    </p>

                    <p>
                      Frequency:{" "}
                      {formatFrequency(
                        recurringTransaction.frequencyUnit,
                        recurringTransaction.frequencyInterval,
                      )}
                    </p>

                    <p>
                      Next payment:{" "}
                      {formatDate(recurringTransaction.nextOccurrence)}
                    </p>

                    <p>
                      Status:{" "}
                      {!recurringTransaction.active
                        ? "Inactive"
                        : recurringTransaction.skipNextOccurrence
                          ? "Active — next occurrence marked to skip"
                          : "Active"}
                    </p>

                    <button
                      type="button"
                      onClick={() => handleEdit(recurringTransaction)}
                      disabled={isProcessing || isSaving}
                    >
                      Edit
                    </button>

                    {recurringTransaction.active &&
                      (recurringTransaction.skipNextOccurrence ? (
                        <button
                          type="button"
                          onClick={() =>
                            void handleUndoSkip(recurringTransaction.id)
                          }
                          disabled={isProcessing}
                        >
                          {isProcessing ? "Processing..." : "Undo Skip"}
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() =>
                            void handleSkipNext(recurringTransaction.id)
                          }
                          disabled={isProcessing}
                        >
                          {isProcessing
                            ? "Processing..."
                            : "Skip Next Transaction"}
                        </button>
                      ))}

                    <button
                      type="button"
                      onClick={() => void handleDelete(recurringTransaction.id)}
                      disabled={isProcessing}
                    >
                      Delete
                    </button>
                  </article>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </section>
  );
}
