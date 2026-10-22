/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useRef, useState } from "react";
import {
  ArrowLeftRight,
  ArrowDownLeft,
  ArrowUpRight,
  CalendarClock,
  CircleAlert,
  Plus,
  X,
} from "lucide-react";

import { createTransaction } from "../api/transactionApi";
import type { Account } from "../types/account";
import type { Category } from "../types/category";
import type { Transaction } from "../types/transaction";
import type { AccountBalance } from "../api/analyticsApi";
import type {
  CreateTransactionRequest,
  TransactionType,
} from "../types/transaction";

type AddTransactionFormProps = {
  accounts: Account[];
  categories: Category[];
  onTransactionCreated: () => Promise<void>;
  accountBalances?: AccountBalance[];
  duplicateTransaction?: Transaction | null;
  onDuplicateConsumed?: () => void;
  openSignal?: number;
  hideTrigger?: boolean;
};

function getLocalDateTimeValue(): string {
  const now = new Date();
  const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);

  return localDate.toISOString().slice(0, 16);
}

function formatMoney(amount: number, currency = "INR"): string {
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${currency} ${amount.toFixed(2)}`;
  }
}

function getCurrencySymbol(currency = "INR"): string {
  try {
    return (
      new Intl.NumberFormat("en", {
        style: "currency",
        currency,
      })
        .formatToParts(0)
        .find((part) => part.type === "currency")?.value ?? "₹"
    );
  } catch {
    return "₹";
  }
}

function AddTransactionForm({
  accounts,
  categories,
  onTransactionCreated,
  accountBalances = [],
  duplicateTransaction = null,
  onDuplicateConsumed,
  openSignal = 0,
  hideTrigger = false,
}: AddTransactionFormProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [accountId, setAccountId] = useState("");
  const [destinationAccountId, setDestinationAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [occurredAt, setOccurredAt] = useState(getLocalDateTimeValue());

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const dialogRef = useRef<HTMLElement>(null);
  const firstInputRef = useRef<HTMLSelectElement>(null);
  const openerRef = useRef<HTMLElement | null>(null);

  const parentCategoryIds = new Set(
    categories
      .filter((category) => category.parentId !== null)
      .map((category) => category.parentId),
  );

  const availableCategories = categories.filter(
    (category) => category.type === type && !parentCategoryIds.has(category.id),
  );

  const sourceAccount = accounts.find((account) => account.id === accountId);

  const destinationAccount = accounts.find(
    (account) => account.id === destinationAccountId,
  );

  const destinationBalance = accountBalances.find(
    (balance) => balance.accountId === destinationAccountId,
  )?.balance;

  const destinationAccounts = accounts.filter(
    (account) =>
      account.id !== accountId &&
      account.type !== "GOAL" &&
      (!sourceAccount?.currency ||
        account.currency === sourceAccount.currency) &&
      (sourceAccount?.type !== "CREDIT_CARD" || account.type !== "CREDIT_CARD"),
  );

  function openModal() {
    openerRef.current =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;

    setError("");
    setSuccess("");
    setIsOpen(true);
  }

  function closeModal() {
    if (isSubmitting) return;

    setIsOpen(false);
    setError("");
  }

  const previousOpenSignal = useRef(openSignal);

  useEffect(() => {
    if (openSignal !== previousOpenSignal.current) {
      previousOpenSignal.current = openSignal;

      if (openSignal > 0) {
        openModal();
      }
    }
  }, [openSignal]);

  useEffect(() => {
    if (!duplicateTransaction) return;

    const transaction = duplicateTransaction;

    setType(transaction.type);
    setAccountId(transaction.accountId);
    setDestinationAccountId(transaction.destinationAccountId ?? "");
    setCategoryId(transaction.categoryId ?? "");
    setAmount(String(transaction.amount));
    setDescription(transaction.description ?? "");

    const parsedDate = new Date(transaction.occurredAt);
    const localDate = new Date(
      parsedDate.getTime() - parsedDate.getTimezoneOffset() * 60_000,
    );

    setOccurredAt(localDate.toISOString().slice(0, 16));
    setError("");
    setSuccess("");
    setIsOpen(true);
    onDuplicateConsumed?.();
  }, [duplicateTransaction, onDuplicateConsumed]);

  useEffect(() => {
    if (!isOpen) return;

    firstInputRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();

        if (!isSubmitting) {
          setIsOpen(false);
          setError("");
        }

        return;
      }

      if (event.key !== "Tab") return;

      const dialog = dialogRef.current;
      if (!dialog) return;

      const focusableElements = Array.from(
        dialog.querySelectorAll<HTMLElement>(
          'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ),
      );

      if (focusableElements.length === 0) {
        event.preventDefault();
        dialog.focus();
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (
        event.shiftKey &&
        (document.activeElement === firstElement ||
          !dialog.contains(document.activeElement))
      ) {
        event.preventDefault();
        lastElement.focus();
      } else if (
        !event.shiftKey &&
        (document.activeElement === lastElement ||
          !dialog.contains(document.activeElement))
      ) {
        event.preventDefault();
        firstElement.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);

      if (openerRef.current?.isConnected) {
        openerRef.current.focus();
      }
    };
  }, [isOpen, isSubmitting]);

  function handleTypeChange(newType: TransactionType) {
    setType(newType);
    setCategoryId("");
    setDestinationAccountId("");
    setError("");
  }

  function resetForm() {
    setType("EXPENSE");
    setAccountId("");
    setDestinationAccountId("");
    setCategoryId("");
    setAmount("");
    setDescription("");
    setOccurredAt(getLocalDateTimeValue());
    setError("");
  }

  function handleAmountChange(value: string) {
    // Allow an empty field while typing, but reject non-decimal input
    // and more than two decimal places.
    if (value === "" || /^\d*(\.\d{0,2})?$/.test(value)) {
      setAmount(value);
      setError("");
    }
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) return;

    setError("");
    setSuccess("");

    if (!accountId) {
      setError("Please select an account.");
      return;
    }

    const trimmedAmount = amount.trim();
    const parsedAmount = Number(trimmedAmount);

    if (
      trimmedAmount === "" ||
      !Number.isFinite(parsedAmount) ||
      parsedAmount <= 0 ||
      !/^\d+(\.\d{1,2})?$/.test(trimmedAmount)
    ) {
      setError(
        "Enter an amount greater than zero with at most 2 decimal places.",
      );
      return;
    }

    if (!occurredAt || Number.isNaN(new Date(occurredAt).getTime())) {
      setError("Please select a valid date and time.");
      return;
    }

    if (occurredAt > getLocalDateTimeValue()) {
      setError("Transaction date and time cannot be in the future.");
      return;
    }

    if (type === "TRANSFER") {
      if (!destinationAccountId) {
        setError("Please select a destination account.");
        return;
      }

      if (destinationAccountId === accountId) {
        setError("Source and destination accounts must be different.");
        return;
      }

      const destination = accounts.find(
        (account) => account.id === destinationAccountId,
      );

      if (!destination || destination.type === "GOAL") {
        setError("Please select a valid destination account.");
        return;
      }

      if (sourceAccount?.type === "CREDIT_CARD") {
        setError("Credit cards cannot be used as the source of a transfer.");
        return;
      }

      if (sourceAccount && sourceAccount.currency !== destination.currency) {
        setError("Source and destination accounts must use the same currency.");
        return;
      }

      if (
        destination.type === "CREDIT_CARD" &&
        destinationBalance !== undefined
      ) {
        if (destinationBalance <= 0) {
          setError("This credit card has no outstanding balance to pay.");
          return;
        }

        if (parsedAmount > destinationBalance) {
          setError(
            `Payment cannot exceed the outstanding balance of ${formatMoney(
              destinationBalance,
              destination.currency || "INR",
            )}.`,
          );
          return;
        }
      }
    } else {
      if (!categoryId) {
        setError("Please select a category.");
        return;
      }

      if (!availableCategories.some((category) => category.id === categoryId)) {
        setError("Please select a valid category for this transaction type.");
        return;
      }
    }

    const request: CreateTransactionRequest = {
      accountId,
      type,
      amount: parsedAmount,
      // datetime-local is interpreted in the user's local timezone;
      // ISO conversion includes the correct UTC offset.
      occurredAt: new Date(occurredAt).toISOString(),
    };

    if (type === "TRANSFER") {
      request.destinationAccountId = destinationAccountId;
    } else {
      request.categoryId = categoryId;
    }

    if (description.trim()) {
      request.description = description.trim();
    }

    setIsSubmitting(true);

    try {
      await createTransaction(request);

      resetForm();
      setIsOpen(false);
      setSuccess("Transaction created successfully.");

      try {
        await onTransactionCreated();
      } catch {
        setError(
          "Transaction was created, but the list could not refresh. Please refresh the page.",
        );
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to create transaction. Please try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="add-transaction">
      {success && (
        <p
          className="transaction-form-success"
          role="status"
          aria-live="polite"
        >
          <span>{success}</span>
          <button
            type="button"
            onClick={() => setSuccess("")}
            aria-label="Dismiss success message"
          >
            <X size={15} aria-hidden="true" />
          </button>
        </p>
      )}

      {!hideTrigger && (
        <button
          type="button"
          className="add-transaction-trigger"
          onClick={openModal}
        >
          <Plus size={17} aria-hidden="true" />
          Add transaction
        </button>
      )}

      {isOpen && (
        <div
          className="account-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeModal();
            }
          }}
        >
          <section
            ref={dialogRef}
            className={`account-modal transaction-modal ${
              type === "TRANSFER"
                ? "transaction-modal-transfer"
                : "transaction-modal-standard"
            }`}
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-transaction-title"
            aria-describedby="add-transaction-description"
            tabIndex={-1}
          >
            <div className="account-modal-header">
              <div>
                <p className="eyebrow">MONEY MOVEMENT</p>
                <h2 id="add-transaction-title">Add transaction</h2>
                <p
                  id="add-transaction-description"
                  className="account-modal-description"
                >
                  Record income, expenses, or transfers between accounts.
                </p>
              </div>

              <button
                type="button"
                className="icon-button"
                onClick={closeModal}
                disabled={isSubmitting}
                aria-label="Close add transaction dialog"
              >
                ×
              </button>
            </div>

            <form className="account-form" onSubmit={handleSubmit} noValidate>
              {error && (
                <div
                  className="transaction-form-error"
                  role="alert"
                  aria-live="assertive"
                >
                  <CircleAlert size={17} aria-hidden="true" />
                  <span>{error}</span>
                </div>
              )}

              <label className="transaction-amount-field">
                Amount
                <div className="transaction-amount-input">
                  <span className="transaction-currency-symbol">
                    {getCurrencySymbol(
                      sourceAccount?.currency ?? accounts[0]?.currency ?? "INR",
                    )}
                  </span>
                  <input
                    type="text"
                    inputMode="decimal"
                    autoComplete="off"
                    placeholder="0.00"
                    value={amount}
                    onChange={(event) => handleAmountChange(event.target.value)}
                    aria-label="Transaction amount"
                    aria-describedby="transaction-amount-hint"
                    required
                    disabled={isSubmitting}
                  />
                </div>
                <span id="transaction-amount-hint" className="field-hint">
                  Use up to two decimal places.
                </span>
              </label>

              <fieldset className="transaction-type-picker">
                <legend>Transaction type</legend>

                <div className="transaction-type-options">
                  <button
                    type="button"
                    className={`transaction-type-option ${
                      type === "EXPENSE" ? "is-selected expense-selected" : ""
                    }`}
                    onClick={() => handleTypeChange("EXPENSE")}
                    disabled={isSubmitting}
                    aria-pressed={type === "EXPENSE"}
                  >
                    <ArrowUpRight size={18} aria-hidden="true" />
                    <span>Expense</span>
                  </button>

                  <button
                    type="button"
                    className={`transaction-type-option ${
                      type === "INCOME" ? "is-selected income-selected" : ""
                    }`}
                    onClick={() => handleTypeChange("INCOME")}
                    disabled={isSubmitting}
                    aria-pressed={type === "INCOME"}
                  >
                    <ArrowDownLeft size={18} aria-hidden="true" />
                    <span>Income</span>
                  </button>

                  <button
                    type="button"
                    className={`transaction-type-option ${
                      type === "TRANSFER" ? "is-selected transfer-selected" : ""
                    }`}
                    onClick={() => handleTypeChange("TRANSFER")}
                    disabled={isSubmitting}
                    aria-pressed={type === "TRANSFER"}
                  >
                    <ArrowLeftRight size={18} aria-hidden="true" />
                    <span>Transfer</span>
                  </button>
                </div>
              </fieldset>

              <div className="transaction-form-grid">
                <label>
                  {type === "TRANSFER" ? "From account" : "Account"}
                  <select
                    ref={firstInputRef}
                    value={accountId}
                    onChange={(event) => {
                      setAccountId(event.target.value);
                      setDestinationAccountId("");
                      setError("");
                    }}
                    disabled={isSubmitting || accounts.length === 0}
                    required
                  >
                    <option value="">Select account</option>
                    {accounts
                      .filter(
                        (account) =>
                          account.type !== "GOAL" &&
                          (type !== "TRANSFER" ||
                            account.type !== "CREDIT_CARD"),
                      )
                      .map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.name} ·{" "}
                          {formatMoney(
                            accountBalances.find(
                              (balance) => balance.accountId === account.id,
                            )?.balance ?? account.openingBalance,
                            account.currency || "INR",
                          )}
                        </option>
                      ))}
                  </select>

                  {accounts.filter(
                    (account) =>
                      account.type !== "GOAL" &&
                      (type !== "TRANSFER" || account.type !== "CREDIT_CARD"),
                  ).length === 0 && (
                    <span className="field-hint">
                      Create an eligible account first.
                    </span>
                  )}
                </label>

                {type === "TRANSFER" ? (
                  <label>
                    To account
                    <select
                      value={destinationAccountId}
                      onChange={(event) => {
                        setDestinationAccountId(event.target.value);
                        setError("");
                      }}
                      disabled={
                        isSubmitting || destinationAccounts.length === 0
                      }
                      required
                    >
                      <option value="">Select destination account</option>
                      {destinationAccounts.map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.name} ·{" "}
                          {formatMoney(
                            accountBalances.find(
                              (balance) => balance.accountId === account.id,
                            )?.balance ?? account.openingBalance,
                            account.currency || "INR",
                          )}
                        </option>
                      ))}
                    </select>
                    {destinationAccount?.type === "CREDIT_CARD" && (
                      <span className="field-hint">
                        Outstanding:{" "}
                        {formatMoney(
                          destinationBalance ?? 0,
                          destinationAccount.currency || "INR",
                        )}
                        . Payment cannot exceed this amount.
                      </span>
                    )}
                    <span className="field-hint">
                      Transfers require different accounts in the same currency.
                    </span>
                  </label>
                ) : (
                  <label>
                    Category
                    <select
                      value={categoryId}
                      onChange={(event) => {
                        setCategoryId(event.target.value);
                        setError("");
                      }}
                      disabled={
                        isSubmitting || availableCategories.length === 0
                      }
                      required
                    >
                      <option value="">Select category</option>
                      {availableCategories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                    {availableCategories.length === 0 && (
                      <span className="field-hint">
                        Create a category for this transaction type first.
                      </span>
                    )}
                  </label>
                )}

                <label>
                  Date and time
                  <div className="transaction-date-input">
                    <CalendarClock size={17} aria-hidden="true" />
                    <input
                      type="datetime-local"
                      value={occurredAt}
                      max={getLocalDateTimeValue()}
                      onChange={(event) => {
                        setOccurredAt(event.target.value);
                        setError("");
                      }}
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                </label>

                <label>
                  Description
                  <input
                    type="text"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="e.g. Groceries, salary…"
                    maxLength={255}
                    disabled={isSubmitting}
                  />
                </label>
              </div>

              <div className="account-form-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={closeModal}
                  disabled={isSubmitting}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "Saving…" : "Save transaction"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}

export default AddTransactionForm;
