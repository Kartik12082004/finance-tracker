import { useEffect, useState } from "react";
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
  return amount.toFixed(2);
}

function formatQuantity(quantity: number | null): string {
  return quantity === null ? "Not tracked" : quantity.toFixed(8);
}

function formatPrice(price: number | null): string {
  return price === null ? "Not tracked" : price.toFixed(4);
}

function formatDateTime(dateTime: string): string {
  return new Date(dateTime).toLocaleString("en-GB");
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

export default function Investments({ accounts }: InvestmentsProps) {
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [type, setType] = useState<InvestmentType>("STOCK");
  const [isCreating, setIsCreating] = useState(false);

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

  async function loadPortfolio() {
    const portfolioResponse = await getPortfolio();
    setPortfolio(portfolioResponse);
  }

  useEffect(() => {
    async function loadInvestments() {
      try {
        setIsLoading(true);
        setError(null);

        await loadPortfolio();
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load investments.",
        );
      } finally {
        setIsLoading(false);
      }
    }

    void loadInvestments();
  }, []);

  async function refreshPortfolio() {
    const updatedPortfolio = await getPortfolio();
    setPortfolio(updatedPortfolio);
  }

  async function handleCreateInvestment(
    event: React.FormEvent<HTMLFormElement>,
  ) {
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
    event: React.FormEvent<HTMLFormElement>,
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

    if (!confirmed) {
      return;
    }

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
    setTransactionForm(createEmptyTransactionForm);
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

  async function handleTransactionSubmit(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    if (transactionInvestmentId === null) {
      return;
    }

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

    try {
      setIsSubmittingTransaction(true);
      setError(null);

      const occurredAt = new Date(transactionForm.occurredAt).toISOString();

      if (transactionType === "BUY") {
        await buyInvestment(transactionInvestmentId, {
          accountId: transactionForm.accountId,
          amount: parsedAmount,
          quantity: parsedQuantity,
          pricePerUnit: parsedPricePerUnit,
          occurredAt,
        });
      } else {
        await sellInvestment(transactionInvestmentId, {
          accountId: transactionForm.accountId,
          amount: parsedAmount,
          quantity: parsedQuantity,
          pricePerUnit: parsedPricePerUnit,
          occurredAt,
        });
      }

      await refreshPortfolio();
      cancelTransaction();

      if (historyInvestmentId === transactionInvestmentId) {
        const updatedTransactions = await getInvestmentTransactions(
          transactionInvestmentId,
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

  if (isLoading) {
    return (
      <section>
        <h2>Investments</h2>
        <p>Loading investments...</p>
      </section>
    );
  }

  return (
    <section>
      <h2>Investments</h2>

      {error && <p>{error}</p>}

      <section>
        <h3>Portfolio Summary</h3>

        <p>Total invested: ₹{formatAmount(portfolio?.totalInvested ?? 0)}</p>

        <p>Current value: ₹{formatAmount(portfolio?.currentValue ?? 0)}</p>

        <p>Gain/Loss: ₹{formatAmount(portfolio?.gainLoss ?? 0)}</p>

        <p>Return: {formatAmount(portfolio?.returnPercentage ?? 0)}%</p>
      </section>

      <section>
        <h3>Create Investment</h3>

        <form onSubmit={handleCreateInvestment}>
          <div>
            <label htmlFor="investment-name">Name</label>

            <input
              id="investment-name"
              type="text"
              maxLength={255}
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="investment-type">Type</label>

            <select
              id="investment-type"
              value={type}
              onChange={(event) =>
                setType(event.target.value as InvestmentType)
              }
            >
              {investmentTypes.map((investmentType) => (
                <option key={investmentType} value={investmentType}>
                  {investmentType}
                </option>
              ))}
            </select>
          </div>

          <button type="submit" disabled={isCreating}>
            {isCreating ? "Creating..." : "Create Investment"}
          </button>
        </form>
      </section>

      <section>
        <h3>Your Investments</h3>

        {investments.length === 0 ? (
          <p>No investments found.</p>
        ) : (
          <ul>
            {investments.map((investment) => (
              <li key={investment.id}>
                <article>
                  <h4>{investment.name}</h4>

                  {editingInvestmentId === investment.id ? (
                    <form
                      onSubmit={(event) =>
                        handleUpdateInvestment(event, investment.id)
                      }
                    >
                      <div>
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
                          onChange={(event) => setEditName(event.target.value)}
                        />
                      </div>

                      <div>
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
                            <option key={investmentType} value={investmentType}>
                              {investmentType}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label
                          htmlFor={`edit-investment-value-${investment.id}`}
                        >
                          Current value
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
                        />
                      </div>

                      <button type="submit" disabled={isUpdating}>
                        {isUpdating ? "Saving..." : "Save Changes"}
                      </button>

                      <button
                        type="button"
                        onClick={cancelEditingInvestment}
                        disabled={isUpdating}
                      >
                        Cancel
                      </button>
                    </form>
                  ) : (
                    <>
                      <p>Type: {investment.type}</p>

                      <p>Quantity: {formatQuantity(investment.quantity)}</p>

                      <p>
                        Average purchase price: ₹
                        {formatPrice(investment.averagePurchasePrice)}
                      </p>

                      <p>
                        Current value: ₹{formatAmount(investment.currentValue)}
                      </p>

                      <button
                        type="button"
                        onClick={() => startTransaction(investment.id, "BUY")}
                      >
                        Buy
                      </button>

                      <button
                        type="button"
                        onClick={() => startTransaction(investment.id, "SELL")}
                      >
                        Sell
                      </button>

                      <button
                        type="button"
                        onClick={() => startEditingInvestment(investment)}
                      >
                        Edit
                      </button>

                      <button
                        type="button"
                        onClick={() => void handleDeleteInvestment(investment)}
                        disabled={deletingInvestmentId === investment.id}
                      >
                        {deletingInvestmentId === investment.id
                          ? "Deleting..."
                          : "Delete"}
                      </button>

                      <button
                        type="button"
                        onClick={() => void handleViewHistory(investment.id)}
                      >
                        {historyInvestmentId === investment.id
                          ? "Hide Transactions"
                          : "View Transactions"}
                      </button>
                    </>
                  )}

                  {transactionInvestmentId === investment.id && (
                    <section>
                      <h5>
                        {transactionType === "BUY"
                          ? "Buy Investment"
                          : "Sell Investment"}
                      </h5>

                      <form onSubmit={handleTransactionSubmit}>
                        <div>
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
                          <label
                            htmlFor={`transaction-amount-${investment.id}`}
                          >
                            Amount
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
                          />
                        </div>

                        <div>
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

                        <div>
                          <label htmlFor={`transaction-price-${investment.id}`}>
                            Price per unit (optional)
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

                        <div>
                          <label
                            htmlFor={`transaction-occurred-at-${investment.id}`}
                          >
                            Occurred at
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
                          />
                        </div>

                        <button
                          type="submit"
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
                          onClick={cancelTransaction}
                          disabled={isSubmittingTransaction}
                        >
                          Cancel
                        </button>
                      </form>
                    </section>
                  )}

                  {historyInvestmentId === investment.id && (
                    <section>
                      <h5>Transaction History</h5>

                      {isLoadingHistory ? (
                        <p>Loading transactions...</p>
                      ) : investmentTransactions.length === 0 ? (
                        <p>No transactions found.</p>
                      ) : (
                        <ul>
                          {investmentTransactions.map((transaction) => (
                            <li key={transaction.id}>
                              <p>
                                Type: {transactionTypeLabel(transaction.type)}
                              </p>

                              <p>Amount: ₹{formatAmount(transaction.amount)}</p>

                              <p>
                                Quantity: {formatQuantity(transaction.quantity)}
                              </p>

                              <p>
                                Price per unit: ₹
                                {formatPrice(transaction.pricePerUnit)}
                              </p>

                              <p>Account: {transaction.accountId}</p>

                              <p>
                                Occurred at:{" "}
                                {formatDateTime(transaction.occurredAt)}
                              </p>
                            </li>
                          ))}
                        </ul>
                      )}
                    </section>
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
