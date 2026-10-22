import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import type { Account } from "../types/account";
import {
  buyInvestment,
  createInvestment,
  deleteInvestment,
  getInvestmentTransactions,
  getPortfolio,
  sellInvestment,
  updateInvestment,
} from "../api/investmentApi";
import type {
  Investment,
  InvestmentTransaction,
  InvestmentTransactionType,
  InvestmentType,
  Portfolio,
} from "../types/investment";

type InvestmentsProps = {
  accounts: Account[];
};

type TransactionFormState = {
  accountId: string;
  amount: string;
  quantity: string;
  pricePerUnit: string;
  occurredAt: string;
};

const investmentTypes: InvestmentType[] = [
  "STOCK",
  "MUTUAL_FUND",
  "ETF",
  "GOLD",
  "OTHER",
];

function formatAmount(amount: number): string {
  return amount.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatQuantity(quantity: number | null): string {
  return quantity === null
    ? "Not tracked"
    : quantity.toLocaleString("en-IN", {
        maximumFractionDigits: 8,
      });
}

function formatPrice(price: number | null): string {
  return price === null ? "Not tracked" : formatAmount(price);
}

function formatDateTime(dateTime: string): string {
  return new Date(dateTime).toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function createDefaultOccurredAt(): string {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  const localDate = new Date(now.getTime() - offset * 60 * 1000);

  return localDate.toISOString().slice(0, 16);
}

function createEmptyTransactionForm(): TransactionFormState {
  return {
    accountId: "",
    amount: "",
    quantity: "",
    pricePerUnit: "",
    occurredAt: createDefaultOccurredAt(),
  };
}

function transactionTypeLabel(type: InvestmentTransactionType): string {
  return type === "BUY" ? "Buy" : "Sell";
}

function investmentTypeLabel(type: InvestmentType): string {
  return type
    .split("_")
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(" ");
}

export default function Investments({ accounts }: InvestmentsProps) {
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  const [name, setName] = useState("");
  const [type, setType] = useState<InvestmentType>("STOCK");
  const [isCreating, setIsCreating] = useState(false);
  const [isCreateFormOpen, setIsCreateFormOpen] = useState(false);

  const [editingInvestmentId, setEditingInvestmentId] = useState<string | null>(
    null,
  );
  const [editName, setEditName] = useState("");
  const [editType, setEditType] = useState<InvestmentType>("STOCK");
  const [editCurrentValue, setEditCurrentValue] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);

  const [deletingInvestmentId, setDeletingInvestmentId] = useState<
    string | null
  >(null);

  const [transactionInvestmentId, setTransactionInvestmentId] = useState<
    string | null
  >(null);
  const [transactionType, setTransactionType] =
    useState<InvestmentTransactionType>("BUY");
  const [transactionForm, setTransactionForm] = useState<TransactionFormState>(
    createEmptyTransactionForm,
  );
  const [isSubmittingTransaction, setIsSubmittingTransaction] = useState(false);

  const [historyInvestmentId, setHistoryInvestmentId] = useState<string | null>(
    null,
  );
  const [investmentTransactions, setInvestmentTransactions] = useState<
    InvestmentTransaction[]
  >([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadInvestments() {
      setIsLoading(true);
      setLoadError(null);

      try {
        const portfolioResponse = await getPortfolio();

        if (cancelled) return;

        setPortfolio(portfolioResponse);
      } catch {
        if (!cancelled) {
          setLoadError(
            "Could not load investments. Check your connection and try again.",
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadInvestments();

    return () => {
      cancelled = true;
    };
  }, [retryKey]);

  async function refreshPortfolio() {
    try {
      const updatedPortfolio = await getPortfolio();
      setPortfolio(updatedPortfolio);
    } catch {
      setError(
        "Your investment change was saved, but the portfolio could not be refreshed. Reload the portfolio to verify the latest values.",
      );
    }
  }

  async function handleCreateInvestment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!name.trim()) {
      setError("Investment name is required.");
      return;
    }

    try {
      setIsCreating(true);
      setError(null);

      await createInvestment({
        name: name.trim(),
        type,
      });

      setName("");
      setType("STOCK");
      setIsCreateFormOpen(false);

      await refreshPortfolio();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to create investment.",
      );
    } finally {
      setIsCreating(false);
    }
  }

  function startEditingInvestment(investment: Investment) {
    setEditingInvestmentId(investment.id);
    setEditName(investment.name);
    setEditType(investment.type);
    setEditCurrentValue(String(investment.currentValue));
    setError(null);
  }

  function cancelEditingInvestment() {
    setEditingInvestmentId(null);
    setEditName("");
    setEditType("STOCK");
    setEditCurrentValue("");
  }

  async function handleUpdateInvestment(
    event: FormEvent<HTMLFormElement>,
    investmentId: string,
  ) {
    event.preventDefault();

    const parsedCurrentValue = Number(editCurrentValue);

    if (!editName.trim()) {
      setError("Investment name is required.");
      return;
    }

    if (!Number.isFinite(parsedCurrentValue) || parsedCurrentValue < 0) {
      setError("Current value cannot be negative.");
      return;
    }

    try {
      setIsUpdating(true);
      setError(null);

      await updateInvestment(investmentId, {
        name: editName.trim(),
        type: editType,
        currentValue: parsedCurrentValue,
      });

      await refreshPortfolio();
      cancelEditingInvestment();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to update investment.",
      );
    } finally {
      setIsUpdating(false);
    }
  }

  async function handleDeleteInvestment(investment: Investment) {
    const confirmed = window.confirm(
      `Are you sure you want to permanently delete "${investment.name}"? This cannot be undone.`,
    );

    if (!confirmed) return;

    try {
      setDeletingInvestmentId(investment.id);
      setError(null);

      await deleteInvestment(investment.id);

      if (editingInvestmentId === investment.id) {
        cancelEditingInvestment();
      }

      if (transactionInvestmentId === investment.id) {
        setTransactionInvestmentId(null);
      }

      if (historyInvestmentId === investment.id) {
        setHistoryInvestmentId(null);
        setInvestmentTransactions([]);
      }

      await refreshPortfolio();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to delete investment.",
      );
    } finally {
      setDeletingInvestmentId(null);
    }
  }

  function startTransaction(
    investmentId: string,
    selectedTransactionType: InvestmentTransactionType,
  ) {
    setTransactionInvestmentId(investmentId);
    setTransactionType(selectedTransactionType);
    setTransactionForm(createEmptyTransactionForm());
    setError(null);
  }

  function cancelTransaction() {
    setTransactionInvestmentId(null);
    setTransactionForm(createEmptyTransactionForm());
  }

  function updateTransactionField(
    field: keyof TransactionFormState,
    value: string,
  ) {
    setTransactionForm((current) => ({
      ...current,
      [field]: value,
    }));
  }

  async function handleTransactionSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (transactionInvestmentId === null) return;

    if (!transactionForm.accountId) {
      setError("Account is required.");
      return;
    }

    const parsedAmount = Number(transactionForm.amount);

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError("Amount must be greater than zero.");
      return;
    }

    if (!transactionForm.occurredAt) {
      setError("Occurred at is required.");
      return;
    }

    const parsedQuantity =
      transactionForm.quantity.trim() === ""
        ? undefined
        : Number(transactionForm.quantity);

    const parsedPricePerUnit =
      transactionForm.pricePerUnit.trim() === ""
        ? undefined
        : Number(transactionForm.pricePerUnit);

    if (
      parsedQuantity !== undefined &&
      (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0)
    ) {
      setError("Quantity must be greater than zero.");
      return;
    }

    if (
      parsedPricePerUnit !== undefined &&
      (!Number.isFinite(parsedPricePerUnit) || parsedPricePerUnit <= 0)
    ) {
      setError("Price per unit must be greater than zero.");
      return;
    }

    const submittedInvestmentId = transactionInvestmentId;

    try {
      setIsSubmittingTransaction(true);
      setError(null);

      const occurredAt = new Date(transactionForm.occurredAt).toISOString();

      const request = {
        accountId: transactionForm.accountId,
        amount: parsedAmount,
        quantity: parsedQuantity,
        pricePerUnit: parsedPricePerUnit,
        occurredAt,
      };

      if (transactionType === "BUY") {
        await buyInvestment(submittedInvestmentId, request);
      } else {
        await sellInvestment(submittedInvestmentId, request);
      }

      await refreshPortfolio();
      cancelTransaction();

      if (historyInvestmentId === submittedInvestmentId) {
        const updatedTransactions = await getInvestmentTransactions(
          submittedInvestmentId,
        );
        setInvestmentTransactions(updatedTransactions);
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : `Failed to ${transactionType.toLowerCase()} investment.`,
      );
    } finally {
      setIsSubmittingTransaction(false);
    }
  }

  async function handleViewHistory(investmentId: string) {
    if (historyInvestmentId === investmentId) {
      setHistoryInvestmentId(null);
      setInvestmentTransactions([]);
      return;
    }

    try {
      setIsLoadingHistory(true);
      setError(null);

      const transactions = await getInvestmentTransactions(investmentId);

      setHistoryInvestmentId(investmentId);
      setInvestmentTransactions(transactions);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to load investment history.",
      );
    } finally {
      setIsLoadingHistory(false);
    }
  }

  const investments = portfolio?.investments ?? [];
  const totalInvested = portfolio?.totalInvested ?? 0;
  const currentValue = portfolio?.currentValue ?? 0;
  const gainLoss = portfolio?.gainLoss ?? 0;
  const returnPercentage = portfolio?.returnPercentage ?? 0;

  if (isLoading) {
    return (
      <section className="investments-page">
        <header className="investments-page-header">
          <div>
            <p className="investments-eyebrow">Portfolio</p>
            <h2>Investments</h2>
            <p className="investments-subtitle">
              Track your holdings and investment activity.
            </p>
          </div>
        </header>

        <div className="investments-loading" role="status">
          <span className="investments-loading-indicator" />
          <p>Loading your portfolio...</p>
        </div>
      </section>
    );
  }

  if (loadError) {
    return (
      <section className="investments-page">
        <header className="investments-page-header">
          <div>
            <p className="investments-eyebrow">Portfolio</p>
            <h2>Investments</h2>
            <p className="investments-subtitle">
              Track your holdings and investment activity.
            </p>
          </div>
        </header>
        <div className="investment-alert" role="alert">
          <div>
            <strong>Portfolio unavailable</strong>
            <p>{loadError}</p>
          </div>
          <button
            type="button"
            className="investment-button investment-button-secondary"
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
    <section className="investments-page">
      <header className="investments-page-header">
        <div>
          <p className="investments-eyebrow">Portfolio</p>
          <h2>Investments</h2>
          <p className="investments-subtitle">
            Track your holdings, monitor performance, and manage investment
            activity in one place.
          </p>
        </div>

        {!isCreateFormOpen && (
          <button
            type="button"
            className="investment-button investment-button-primary"
            onClick={() => {
              setError(null);
              setIsCreateFormOpen(true);
            }}
          >
            + Create Investment
          </button>
        )}
      </header>

      {loadError && (
        <div className="investment-alert" role="alert">
          <div>
            <strong>Portfolio unavailable</strong>
            <p>{loadError}</p>
          </div>
          <button
            type="button"
            className="investment-button investment-button-secondary"
            onClick={() => setRetryKey((current) => current + 1)}
            disabled={isLoading}
          >
            {isLoading ? "Retrying..." : "Retry"}
          </button>
        </div>
      )}

      {error && (
        <div className="investment-alert" role="alert">
          <p>{error}</p>
          <button
            type="button"
            className="investment-alert-dismiss"
            onClick={() => setError(null)}
            aria-label="Dismiss error"
          >
            ×
          </button>
        </div>
      )}

      <section className="investment-summary-section">
        <div className="investment-section-heading">
          <div>
            <h3>Portfolio Summary</h3>
            <p>Your investment portfolio at a glance.</p>
          </div>
        </div>

        <div className="investment-summary-grid">
          <article className="investment-summary-card">
            <div className="investment-summary-card-top">
              <span className="investment-summary-icon">₹</span>
              <span className="investment-summary-label">Total Invested</span>
            </div>
            <p className="investment-summary-value">
              ₹{formatAmount(totalInvested)}
            </p>
            <p className="investment-summary-caption">
              Capital recorded in your portfolio
            </p>
          </article>

          <article className="investment-summary-card">
            <div className="investment-summary-card-top">
              <span className="investment-summary-icon">↗</span>
              <span className="investment-summary-label">Current Value</span>
            </div>
            <p className="investment-summary-value">
              ₹{formatAmount(currentValue)}
            </p>
            <p className="investment-summary-caption">
              Current portfolio valuation
            </p>
          </article>

          <article
            className={`investment-summary-card ${
              gainLoss > 0
                ? "investment-summary-positive"
                : gainLoss < 0
                  ? "investment-summary-negative"
                  : ""
            }`}
          >
            <div className="investment-summary-card-top">
              <span className="investment-summary-icon">
                {gainLoss > 0 ? "↗" : gainLoss < 0 ? "↘" : "—"}
              </span>
              <span className="investment-summary-label">Gain / Loss</span>
            </div>
            <p className="investment-summary-value">
              {gainLoss < 0 ? "−" : ""}₹{formatAmount(Math.abs(gainLoss))}
            </p>
            <p className="investment-summary-caption">
              Difference from total invested
            </p>
          </article>

          <article
            className={`investment-summary-card ${
              returnPercentage > 0
                ? "investment-summary-positive"
                : returnPercentage < 0
                  ? "investment-summary-negative"
                  : ""
            }`}
          >
            <div className="investment-summary-card-top">
              <span className="investment-summary-icon">%</span>
              <span className="investment-summary-label">Portfolio Return</span>
            </div>
            <p className="investment-summary-value">
              {returnPercentage > 0 ? "+" : ""}
              {formatAmount(returnPercentage)}%
            </p>
            <p className="investment-summary-caption">
              Reported portfolio return
            </p>
          </article>
        </div>
      </section>

      {isCreateFormOpen && (
        <section className="investment-panel investment-create-panel">
          <div className="investment-section-heading">
            <div>
              <h3>Create Investment</h3>
              <p>
                Add an investment to your portfolio before recording
                transactions.
              </p>
            </div>
          </div>

          <form className="investment-form" onSubmit={handleCreateInvestment}>
            <div className="investment-form-grid">
              <div className="investment-field">
                <label htmlFor="investment-name">Investment name</label>
                <input
                  id="investment-name"
                  type="text"
                  maxLength={255}
                  placeholder="e.g. Nifty 50 Index Fund"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  required
                />
              </div>

              <div className="investment-field">
                <label htmlFor="investment-type">Investment type</label>
                <select
                  id="investment-type"
                  value={type}
                  onChange={(event) =>
                    setType(event.target.value as InvestmentType)
                  }
                >
                  {investmentTypes.map((investmentType) => (
                    <option key={investmentType} value={investmentType}>
                      {investmentTypeLabel(investmentType)}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="investment-form-actions">
              <button
                type="submit"
                className="investment-button investment-button-primary"
                disabled={isCreating}
              >
                {isCreating ? "Creating..." : "Create Investment"}
              </button>
              <button
                type="button"
                className="investment-button investment-button-secondary"
                onClick={() => {
                  setIsCreateFormOpen(false);
                  setName("");
                  setType("STOCK");
                  setError(null);
                }}
                disabled={isCreating}
              >
                Cancel
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="investment-holdings-section">
        <div className="investment-section-heading">
          <div>
            <h3>Your Investments</h3>
            <p>Review individual holdings and manage their transactions.</p>
          </div>
          <span className="investment-count">{investments.length}</span>
        </div>

        {investments.length === 0 ? (
          <div className="investment-empty-state">
            <span className="investment-empty-icon">↗</span>
            <h4>Your portfolio starts here</h4>
            <p>
              Add your first investment to start recording holdings and tracking
              portfolio activity.
            </p>
            {!isCreateFormOpen && (
              <button
                type="button"
                className="investment-button investment-button-primary"
                onClick={() => setIsCreateFormOpen(true)}
              >
                Create Your First Investment
              </button>
            )}
          </div>
        ) : (
          <div className="investment-holdings-grid">
            {investments.map((investment) => {
              const isEditing = editingInvestmentId === investment.id;
              const isTransacting = transactionInvestmentId === investment.id;
              const isShowingHistory = historyInvestmentId === investment.id;
              const isDeleting = deletingInvestmentId === investment.id;

              return (
                <article
                  key={investment.id}
                  className="investment-holding-card"
                >
                  <div className="investment-holding-header">
                    <div className="investment-holding-title">
                      <span className="investment-type-icon">
                        {investment.type === "GOLD"
                          ? "Au"
                          : investment.type === "MUTUAL_FUND"
                            ? "MF"
                            : investment.type === "ETF"
                              ? "ETF"
                              : investment.type === "STOCK"
                                ? "ST"
                                : "OT"}
                      </span>
                      <div>
                        <h4>{investment.name}</h4>
                        <span className="investment-type-badge">
                          {investmentTypeLabel(investment.type)}
                        </span>
                      </div>
                    </div>
                    <div className="investment-holding-value">
                      <span>Current value</span>
                      <strong>₹{formatAmount(investment.currentValue)}</strong>
                    </div>
                  </div>

                  {isEditing ? (
                    <form
                      className="investment-inline-form"
                      onSubmit={(event) =>
                        handleUpdateInvestment(event, investment.id)
                      }
                    >
                      <div className="investment-form-grid">
                        <div className="investment-field">
                          <label
                            htmlFor={`edit-investment-name-${investment.id}`}
                          >
                            Name
                          </label>
                          <input
                            id={`edit-investment-name-${investment.id}`}
                            type="text"
                            maxLength={255}
                            value={editName}
                            onChange={(event) =>
                              setEditName(event.target.value)
                            }
                            required
                          />
                        </div>

                        <div className="investment-field">
                          <label
                            htmlFor={`edit-investment-type-${investment.id}`}
                          >
                            Type
                          </label>
                          <select
                            id={`edit-investment-type-${investment.id}`}
                            value={editType}
                            onChange={(event) =>
                              setEditType(event.target.value as InvestmentType)
                            }
                          >
                            {investmentTypes.map((investmentType) => (
                              <option
                                key={investmentType}
                                value={investmentType}
                              >
                                {investmentTypeLabel(investmentType)}
                              </option>
                            ))}
                          </select>
                        </div>

                        <div className="investment-field">
                          <label
                            htmlFor={`edit-investment-value-${investment.id}`}
                          >
                            Current value (₹)
                          </label>
                          <input
                            id={`edit-investment-value-${investment.id}`}
                            type="number"
                            min="0"
                            step="0.0001"
                            value={editCurrentValue}
                            onChange={(event) =>
                              setEditCurrentValue(event.target.value)
                            }
                            required
                          />
                        </div>
                      </div>

                      <div className="investment-form-actions">
                        <button
                          type="submit"
                          className="investment-button investment-button-primary"
                          disabled={isUpdating}
                        >
                          {isUpdating ? "Saving..." : "Save Changes"}
                        </button>
                        <button
                          type="button"
                          className="investment-button investment-button-secondary"
                          onClick={cancelEditingInvestment}
                          disabled={isUpdating}
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  ) : (
                    <>
                      <div className="investment-holding-metrics">
                        <div>
                          <span>Quantity</span>
                          <strong>{formatQuantity(investment.quantity)}</strong>
                        </div>
                        <div>
                          <span>Average purchase price</span>
                          <strong>
                            {investment.averagePurchasePrice === null
                              ? "Not tracked"
                              : `₹${formatPrice(investment.averagePurchasePrice)}`}
                          </strong>
                        </div>
                      </div>

                      <div className="investment-card-actions">
                        <button
                          type="button"
                          className="investment-button investment-button-primary"
                          onClick={() => startTransaction(investment.id, "BUY")}
                          disabled={isDeleting || isSubmittingTransaction}
                        >
                          Buy
                        </button>
                        <button
                          type="button"
                          className="investment-button investment-button-secondary"
                          onClick={() =>
                            startTransaction(investment.id, "SELL")
                          }
                          disabled={isDeleting || isSubmittingTransaction}
                        >
                          Sell
                        </button>
                        <button
                          type="button"
                          className="investment-button investment-button-secondary"
                          onClick={() => startEditingInvestment(investment)}
                          disabled={isDeleting}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="investment-button investment-button-secondary"
                          onClick={() => void handleViewHistory(investment.id)}
                          disabled={isLoadingHistory}
                        >
                          {isShowingHistory ? "Hide History" : "View History"}
                        </button>
                        <button
                          type="button"
                          className="investment-button investment-button-danger"
                          onClick={() =>
                            void handleDeleteInvestment(investment)
                          }
                          disabled={isDeleting}
                        >
                          {isDeleting ? "Deleting..." : "Delete"}
                        </button>
                      </div>
                    </>
                  )}

                  {isTransacting && (
                    <section className="investment-expanded-panel">
                      <div className="investment-expanded-heading">
                        <div>
                          <h5>
                            {transactionType === "BUY"
                              ? "Buy Investment"
                              : "Sell Investment"}
                          </h5>
                          <p>Record the transaction details below.</p>
                        </div>
                      </div>

                      <form
                        className="investment-form"
                        onSubmit={handleTransactionSubmit}
                      >
                        <div className="investment-form-grid">
                          <div className="investment-field">
                            <label
                              htmlFor={`transaction-account-${investment.id}`}
                            >
                              Account
                            </label>
                            <select
                              id={`transaction-account-${investment.id}`}
                              value={transactionForm.accountId}
                              onChange={(event) =>
                                updateTransactionField(
                                  "accountId",
                                  event.target.value,
                                )
                              }
                              required
                            >
                              <option value="">Select account</option>
                              {accounts.map((account) => (
                                <option key={account.id} value={account.id}>
                                  {account.name}
                                </option>
                              ))}
                            </select>
                          </div>

                          <div className="investment-field">
                            <label
                              htmlFor={`transaction-amount-${investment.id}`}
                            >
                              Amount (₹)
                            </label>
                            <input
                              id={`transaction-amount-${investment.id}`}
                              type="number"
                              min="0.01"
                              step="0.01"
                              value={transactionForm.amount}
                              onChange={(event) =>
                                updateTransactionField(
                                  "amount",
                                  event.target.value,
                                )
                              }
                              required
                            />
                          </div>

                          <div className="investment-field">
                            <label
                              htmlFor={`transaction-quantity-${investment.id}`}
                            >
                              Quantity (optional)
                            </label>
                            <input
                              id={`transaction-quantity-${investment.id}`}
                              type="number"
                              min="0.00000001"
                              step="0.00000001"
                              value={transactionForm.quantity}
                              onChange={(event) =>
                                updateTransactionField(
                                  "quantity",
                                  event.target.value,
                                )
                              }
                            />
                          </div>

                          <div className="investment-field">
                            <label
                              htmlFor={`transaction-price-${investment.id}`}
                            >
                              Price per unit (₹, optional)
                            </label>
                            <input
                              id={`transaction-price-${investment.id}`}
                              type="number"
                              min="0.0001"
                              step="0.0001"
                              value={transactionForm.pricePerUnit}
                              onChange={(event) =>
                                updateTransactionField(
                                  "pricePerUnit",
                                  event.target.value,
                                )
                              }
                            />
                          </div>

                          <div className="investment-field investment-field-full">
                            <label
                              htmlFor={`transaction-occurred-at-${investment.id}`}
                            >
                              Transaction date and time
                            </label>
                            <input
                              id={`transaction-occurred-at-${investment.id}`}
                              type="datetime-local"
                              value={transactionForm.occurredAt}
                              onChange={(event) =>
                                updateTransactionField(
                                  "occurredAt",
                                  event.target.value,
                                )
                              }
                              required
                            />
                          </div>
                        </div>

                        <div className="investment-form-actions">
                          <button
                            type="submit"
                            className="investment-button investment-button-primary"
                            disabled={isSubmittingTransaction}
                          >
                            {isSubmittingTransaction
                              ? "Submitting..."
                              : transactionType === "BUY"
                                ? "Submit Buy"
                                : "Submit Sell"}
                          </button>
                          <button
                            type="button"
                            className="investment-button investment-button-secondary"
                            onClick={cancelTransaction}
                            disabled={isSubmittingTransaction}
                          >
                            Cancel
                          </button>
                        </div>
                      </form>
                    </section>
                  )}

                  {isShowingHistory && (
                    <section className="investment-expanded-panel">
                      <div className="investment-expanded-heading">
                        <div>
                          <h5>Transaction History</h5>
                          <p>Recorded buys and sells for this investment.</p>
                        </div>
                      </div>

                      {isLoadingHistory ? (
                        <p className="investment-inline-status">
                          Loading transactions...
                        </p>
                      ) : investmentTransactions.length === 0 ? (
                        <p className="investment-inline-status">
                          No transactions found.
                        </p>
                      ) : (
                        <ul className="investment-history-list">
                          {investmentTransactions.map((transaction) => (
                            <li
                              key={transaction.id}
                              className="investment-history-item"
                            >
                              <div className="investment-history-header">
                                <span
                                  className={`investment-history-type ${
                                    transaction.type === "BUY"
                                      ? "investment-history-buy"
                                      : "investment-history-sell"
                                  }`}
                                >
                                  {transactionTypeLabel(transaction.type)}
                                </span>
                                <strong>
                                  ₹{formatAmount(transaction.amount)}
                                </strong>
                              </div>

                              <div className="investment-history-details">
                                <p>
                                  <span>Quantity</span>
                                  <strong>
                                    {formatQuantity(transaction.quantity)}
                                  </strong>
                                </p>
                                <p>
                                  <span>Price per unit</span>
                                  <strong>
                                    {transaction.pricePerUnit === null
                                      ? "Not tracked"
                                      : `₹${formatPrice(transaction.pricePerUnit)}`}
                                  </strong>
                                </p>
                                <p>
                                  <span>Account</span>
                                  <strong>
                                    {accounts.find(
                                      (account) =>
                                        account.id === transaction.accountId,
                                    )?.name ?? transaction.accountId}
                                  </strong>
                                </p>
                                <p>
                                  <span>Date</span>
                                  <strong>
                                    {formatDateTime(transaction.occurredAt)}
                                  </strong>
                                </p>
                              </div>
                            </li>
                          ))}
                        </ul>
                      )}
                    </section>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>
    </section>
  );
}
