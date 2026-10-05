import { useEffect, useState } from "react";
import {
  createRecurringTransaction,
  deleteRecurringTransaction,
  getRecurringTransactions,
  pauseRecurringTransaction,
  resumeRecurringTransaction,
} from "../api/recurringTransactionApi";
import { getAccounts } from "../api/accountApi";
import { getCategories } from "../api/categoryApi";
import type { Account } from "../types/account";
import type { Category } from "../types/category";
import type {
  CreateRecurringTransactionRequest,
  RecurringFrequencyUnit,
  RecurringTransaction,
} from "../types/recurringTransaction";
import type { TransactionType } from "../types/transaction";

function formatAmount(amount: number): string {
  return amount.toFixed(2);
}

function formatDate(date: string): string {
  return new Date(`${date}T00:00:00`).toLocaleDateString("en-GB");
}

function formatFrequency(
  unit: RecurringFrequencyUnit,
  interval: number,
): string {
  const unitLabel =
    interval === 1 ? unit.toLowerCase() : `${unit.toLowerCase()}s`;

  return `Every ${interval} ${unitLabel}`;
}

export default function RecurringTransactions() {
  const [recurringTransactions, setRecurringTransactions] = useState<
    RecurringTransaction[]
  >([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [frequencyUnit, setFrequencyUnit] =
    useState<RecurringFrequencyUnit>("MONTH");
  const [frequencyInterval, setFrequencyInterval] = useState("1");
  const [nextOccurrence, setNextOccurrence] = useState("");

  useEffect(() => {
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

        setRecurringTransactions(recurringTransactionsResponse);
        setAccounts(accountsResponse);
        setCategories(categoriesResponse);
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "Failed to load recurring transactions.",
        );
      } finally {
        setIsLoading(false);
      }
    }

    void loadData();
  }, []);

  async function handleCreateRecurringTransaction(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    const parsedAmount = Number(amount);
    const parsedFrequencyInterval = Number(frequencyInterval);

    if (!accountId) {
      setError("Account is required.");
      return;
    }

    if (!categoryId) {
      setError("Category is required.");
      return;
    }

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError("Amount must be greater than zero.");
      return;
    }

    if (
      !Number.isInteger(parsedFrequencyInterval) ||
      parsedFrequencyInterval < 1
    ) {
      setError("Frequency interval must be at least 1.");
      return;
    }

    if (!nextOccurrence) {
      setError("Next occurrence is required.");
      return;
    }

    const request: CreateRecurringTransactionRequest = {
      accountId,
      categoryId,
      type,
      amount: parsedAmount,
      description: description.trim(),
      frequencyUnit,
      frequencyInterval: parsedFrequencyInterval,
      nextOccurrence,
    };

    try {
      setIsCreating(true);
      setError(null);

      const createdRecurringTransaction =
        await createRecurringTransaction(request);

      setRecurringTransactions((current) => [
        ...current,
        createdRecurringTransaction,
      ]);

      setAccountId("");
      setCategoryId("");
      setType("EXPENSE");
      setAmount("");
      setDescription("");
      setFrequencyUnit("MONTH");
      setFrequencyInterval("1");
      setNextOccurrence("");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to create recurring transaction.",
      );
    } finally {
      setIsCreating(false);
    }
  }

  async function handlePause(
    recurringTransaction: RecurringTransaction,
    duration: number | null,
    untilResume: boolean,
  ) {
    try {
      setProcessingId(recurringTransaction.id);
      setError(null);

      const updatedRecurringTransaction = await pauseRecurringTransaction(
        recurringTransaction.id,
        duration,
        untilResume,
      );

      setRecurringTransactions((current) =>
        current.map((transaction) =>
          transaction.id === recurringTransaction.id
            ? updatedRecurringTransaction
            : transaction,
        ),
      );
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to pause recurring transaction.",
      );
    } finally {
      setProcessingId(null);
    }
  }

  async function handleResume(recurringTransactionId: string) {
    try {
      setProcessingId(recurringTransactionId);
      setError(null);

      const updatedRecurringTransaction = await resumeRecurringTransaction(
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
          : "Failed to resume recurring transaction.",
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

  function handleTemporaryPause(recurringTransaction: RecurringTransaction) {
    const rawDuration = window.prompt(
      `Pause for how many ${recurringTransaction.frequencyUnit.toLowerCase()}${
        recurringTransaction.frequencyUnit === "WEEK" ||
        recurringTransaction.frequencyUnit === "MONTH" ||
        recurringTransaction.frequencyUnit === "YEAR"
          ? "s"
          : ""
      }?`,
      "1",
    );

    if (rawDuration === null) {
      return;
    }

    const duration = Number(rawDuration);

    if (!Number.isInteger(duration) || duration < 1) {
      setError("Pause duration must be a whole number of at least 1.");
      return;
    }

    void handlePause(recurringTransaction, duration, false);
  }

  const expenseCategories = categories.filter(
    (category) => category.type === "EXPENSE",
  );

  const incomeCategories = categories.filter(
    (category) => category.type === "INCOME",
  );

  const availableCategories =
    type === "EXPENSE" ? expenseCategories : incomeCategories;

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

      {error && <p>{error}</p>}

      <section>
        <h3>Create Recurring Transaction</h3>

        <form onSubmit={handleCreateRecurringTransaction}>
          <div>
            <label htmlFor="recurring-account">Account</label>

            <select
              id="recurring-account"
              value={accountId}
              onChange={(event) => setAccountId(event.target.value)}
            >
              <option value="">Select account</option>

              {accounts.map((account) => (
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
            >
              <option value="">Select category</option>

              {availableCategories.map((category) => (
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
            <label htmlFor="recurring-frequency-unit">Frequency</label>

            <select
              id="recurring-frequency-unit"
              value={frequencyUnit}
              onChange={(event) =>
                setFrequencyUnit(event.target.value as RecurringFrequencyUnit)
              }
            >
              <option value="WEEK">Week</option>
              <option value="MONTH">Month</option>
              <option value="YEAR">Year</option>
            </select>
          </div>

          <div>
            <label htmlFor="recurring-frequency-interval">Every</label>

            <input
              id="recurring-frequency-interval"
              type="number"
              min="1"
              step="1"
              value={frequencyInterval}
              onChange={(event) => setFrequencyInterval(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="recurring-next-occurrence">Next occurrence</label>

            <input
              id="recurring-next-occurrence"
              type="date"
              value={nextOccurrence}
              onChange={(event) => setNextOccurrence(event.target.value)}
            />
          </div>

          <button type="submit" disabled={isCreating}>
            {isCreating ? "Creating..." : "Create Recurring Transaction"}
          </button>
        </form>
      </section>

      <section>
        <h3>Your Recurring Transactions</h3>

        {recurringTransactions.length === 0 ? (
          <p>No recurring transactions found.</p>
        ) : (
          <ul>
            {recurringTransactions.map((recurringTransaction) => {
              const isProcessing = processingId === recurringTransaction.id;

              const isTemporarilyPaused =
                recurringTransaction.active &&
                recurringTransaction.pausedUntil !== null;

              const isIndefinitelyPaused =
                !recurringTransaction.active &&
                recurringTransaction.pausedUntil === null;

              return (
                <li key={recurringTransaction.id}>
                  <article>
                    <h4>{recurringTransaction.categoryName}</h4>

                    <p>Type: {recurringTransaction.type}</p>

                    <p>Amount: ₹{formatAmount(recurringTransaction.amount)}</p>

                    <p>
                      Account:{" "}
                      {accounts.find(
                        (account) =>
                          account.id === recurringTransaction.accountId,
                      )?.name ?? recurringTransaction.accountId}
                    </p>

                    {recurringTransaction.description && (
                      <p>Description: {recurringTransaction.description}</p>
                    )}

                    <p>
                      Frequency:{" "}
                      {formatFrequency(
                        recurringTransaction.frequencyUnit,
                        recurringTransaction.frequencyInterval,
                      )}
                    </p>

                    <p>
                      Next occurrence:{" "}
                      {formatDate(recurringTransaction.nextOccurrence)}
                    </p>

                    <p>
                      Status:{" "}
                      {recurringTransaction.active
                        ? recurringTransaction.pausedUntil
                          ? `Paused until ${formatDate(
                              recurringTransaction.pausedUntil,
                            )}`
                          : "Active"
                        : "Paused until manually resumed"}
                    </p>

                    <div>
                      {recurringTransaction.active && !isTemporarilyPaused && (
                        <>
                          <button
                            type="button"
                            disabled={isProcessing}
                            onClick={() =>
                              handleTemporaryPause(recurringTransaction)
                            }
                          >
                            {isProcessing
                              ? "Processing..."
                              : "Pause Temporarily"}
                          </button>

                          <button
                            type="button"
                            disabled={isProcessing}
                            onClick={() =>
                              void handlePause(recurringTransaction, null, true)
                            }
                          >
                            {isProcessing
                              ? "Processing..."
                              : "Pause Until I Resume"}
                          </button>
                        </>
                      )}

                      {(isTemporarilyPaused || isIndefinitelyPaused) && (
                        <button
                          type="button"
                          disabled={isProcessing}
                          onClick={() =>
                            void handleResume(recurringTransaction.id)
                          }
                        >
                          {isProcessing ? "Processing..." : "Resume"}
                        </button>
                      )}

                      <button
                        type="button"
                        disabled={isProcessing}
                        onClick={() =>
                          void handleDelete(recurringTransaction.id)
                        }
                      >
                        {isProcessing ? "Processing..." : "Delete"}
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
