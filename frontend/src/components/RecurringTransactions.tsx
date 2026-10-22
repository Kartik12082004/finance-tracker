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
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
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
        setLoadError(null);
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
      } catch {
        if (!cancelled) {
          setLoadError(
            "Could not load recurring transactions. Check your connection and try again.",
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
  }, [retryKey]);
  function resetForm() {
    setEditingId(null);
    setAccountId("");
    setCategoryId("");
    setType("EXPENSE");
    setAmount("");
    setDescription("");
    setPaymentDate("");
    setIsFormOpen(false);
  }
  function handleCreate() {
    resetForm();
    setError(null);
    setIsFormOpen(true);
  }
  function handleEdit(recurringTransaction: RecurringTransaction) {
    setEditingId(recurringTransaction.id);
    setIsFormOpen(true);
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
      !/^\d+(?:\.\d{1,2})?$/.test(amount.trim())
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
      <section className="recurring-page">
        <h2>Recurring Transactions</h2>
        <p role="status">Loading recurring transactions...</p>
      </section>
    );
  }
  if (loadError) {
    return (
      <section className="recurring-page">
        <header className="recurring-heading">
          <div>
            <h2>Recurring Transactions</h2>
            <p className="recurring-subtitle">
              Manage scheduled monthly income and expenses in one place.
            </p>
          </div>
        </header>
        <div className="recurring-load-error" role="alert">
          <p>{loadError}</p>
          <button
            type="button"
            className="recurring-secondary-button"
            onClick={() => setRetryKey((current) => current + 1)}
            disabled={isLoading}
          >
            Retry loading recurring transactions
          </button>
        </div>
      </section>
    );
  }
  return (
    <section className="recurring-page">
      <header className="recurring-heading">
        <div>
          <h2>Recurring Transactions</h2>
          <p className="recurring-subtitle">
            Manage scheduled monthly income and expenses in one place.
          </p>
        </div>
        {!isFormOpen && (
          <button
            type="button"
            className="recurring-primary-button"
            onClick={handleCreate}
            disabled={isSaving}
          >
            Create Payment
          </button>
        )}
      </header>
      {loadError && (
        <div role="alert">
          <p>{loadError}</p>
          <button
            type="button"
            onClick={() => setRetryKey((current) => current + 1)}
          >
            Retry loading recurring transactions
          </button>
        </div>
      )}
      {error && <p role="alert">{error}</p>}
      {isFormOpen && (
        <section className="recurring-form-card">
          <div className="recurring-form-heading">
            <div>
              <h3>
                {editingId
                  ? "Edit Recurring Payment"
                  : "Create Recurring Payment"}
              </h3>
              <p>
                Set a monthly payment and choose its first payment date. Future
                payments follow that calendar day where possible.
              </p>
            </div>
          </div>
          <form className="recurring-form" onSubmit={handleSave}>
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
            <div className="recurring-form-actions">
              <button
                type="submit"
                className="recurring-primary-button"
                disabled={isSaving}
              >
                {isSaving
                  ? "Saving..."
                  : editingId
                    ? "Save Changes"
                    : "Create Recurring Payment"}
              </button>
              <button
                type="button"
                className="recurring-secondary-button"
                onClick={resetForm}
                disabled={isSaving}
              >
                Cancel
              </button>
            </div>
          </form>
        </section>
      )}
      <section className="recurring-payments-section">
        <div className="recurring-section-heading">
          <div>
            <h3>Your Recurring Transactions</h3>
            <p>Review upcoming income and expenses and manage each schedule.</p>
          </div>
          <span className="recurring-count">
            {recurringTransactions.length}
          </span>
        </div>
        {recurringTransactions.length === 0 ? (
          <p className="recurring-empty-state">
            No recurring payments yet. Create a payment to start tracking your
            schedule.
          </p>
        ) : (
          <ul className="recurring-list">
            {recurringTransactions.map((recurringTransaction) => {
              const isProcessing = processingId === recurringTransaction.id;
              return (
                <li
                  key={recurringTransaction.id}
                  className="recurring-list-item"
                >
                  <article className="recurring-card">
                    <div className="recurring-card-header">
                      <div>
                        <h4>
                          {recurringTransaction.description?.trim() ||
                            recurringTransaction.categoryName}
                        </h4>
                        <p className="recurring-card-category">
                          {recurringTransaction.categoryName} ·{" "}
                          {recurringTransaction.type === "EXPENSE"
                            ? "Expense"
                            : "Income"}
                        </p>
                      </div>
                      <span
                        className={`recurring-status ${
                          !recurringTransaction.active
                            ? "recurring-status-inactive"
                            : recurringTransaction.skipNextOccurrence
                              ? "recurring-status-skipped"
                              : "recurring-status-active"
                        }`}
                      >
                        {!recurringTransaction.active
                          ? "Inactive"
                          : recurringTransaction.skipNextOccurrence
                            ? "Next payment skipped"
                            : "Active"}
                      </span>
                    </div>
                    <div className="recurring-card-details">
                      <p>Type: {recurringTransaction.type}</p>
                      <p>
                        Amount: ₹{formatAmount(recurringTransaction.amount)}
                      </p>
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
                    </div>
                    <div className="recurring-card-actions">
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
                        className="recurring-danger-button"
                        onClick={() =>
                          void handleDelete(recurringTransaction.id)
                        }
                        disabled={isProcessing}
                      >
                        Delete
                      </button>
                    </div>
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
