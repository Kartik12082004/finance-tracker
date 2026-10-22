import { useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  Ban,
  CircleAlert,
  Copy,
  MoreHorizontal,
  Search,
  Wallet,
  X,
} from "lucide-react";
import type { Account } from "../types/account";
import type { Category } from "../types/category";
import type { Transaction, TransactionType } from "../types/transaction";

type Props = {
  transactions: Transaction[];
  accounts: Account[];
  categories: Category[];
  onVoidTransaction: (transactionId: string) => Promise<void>;
  onDuplicateTransaction: (transaction: Transaction) => void;
  onAddTransaction: () => void;
};

const PAGE_SIZE = 10;

function getAccountById(accounts: Account[], id: string | null) {
  return accounts.find((account) => account.id === id);
}

function getAccountNameById(accounts: Account[], id: string | null) {
  return getAccountById(accounts, id)?.name ?? "Unknown account";
}

function getCategoryNameById(categories: Category[], id: string | null) {
  return id
    ? (categories.find((category) => category.id === id)?.name ??
        "Unknown category")
    : "Uncategorized";
}

function formatMoney(amount: number, currency?: string) {
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: currency || "INR",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${currency ?? "₹"} ${amount.toFixed(2)}`;
  }
}

function formatDate(value: string) {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "Invalid date"
    : date.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
}

function formatTypeLabel(type: TransactionType) {
  return type === "INCOME"
    ? "Income"
    : type === "EXPENSE"
      ? "Expense"
      : "Transfer";
}

function getTransactionTitle(transaction: Transaction) {
  return transaction.description?.trim() || formatTypeLabel(transaction.type);
}

function getTransferLabel(transaction: Transaction, accounts: Account[]) {
  return `${getAccountNameById(accounts, transaction.accountId)} → ${getAccountNameById(
    accounts,
    transaction.destinationAccountId,
  )}`;
}

function TransactionList({
  transactions,
  accounts,
  categories,
  onVoidTransaction,
  onDuplicateTransaction,
  onAddTransaction,
}: Props) {
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [accountFilter, setAccountFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("POSTED");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [showVoided, setShowVoided] = useState(false);
  const [page, setPage] = useState(1);
  const [menuId, setMenuId] = useState<string | null>(null);
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [voidingId, setVoidingId] = useState<string | null>(null);
  const [error, setError] = useState("");

  const filtered = useMemo(
    () =>
      transactions
        .filter((transaction) => showVoided || transaction.status !== "VOIDED")
        .filter(
          (transaction) =>
            statusFilter === "ALL" || transaction.status === statusFilter,
        )
        .filter(
          (transaction) =>
            typeFilter === "ALL" || transaction.type === typeFilter,
        )
        .filter(
          (transaction) =>
            accountFilter === "ALL" ||
            transaction.accountId === accountFilter ||
            transaction.destinationAccountId === accountFilter,
        )
        .filter(
          (transaction) =>
            categoryFilter === "ALL" ||
            transaction.categoryId === categoryFilter,
        )
        .filter(
          (transaction) =>
            !fromDate || transaction.occurredAt.slice(0, 10) >= fromDate,
        )
        .filter(
          (transaction) =>
            !toDate || transaction.occurredAt.slice(0, 10) <= toDate,
        )
        .filter((transaction) => {
          const searchableText = [
            transaction.description ?? "",
            getAccountNameById(accounts, transaction.accountId),
            getAccountNameById(accounts, transaction.destinationAccountId),
            getCategoryNameById(categories, transaction.categoryId),
          ]
            .join(" ")
            .toLowerCase();

          return searchableText.includes(search.trim().toLowerCase());
        })
        .sort(
          (a, b) =>
            new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime(),
        ),
    [
      transactions,
      showVoided,
      statusFilter,
      typeFilter,
      accountFilter,
      categoryFilter,
      fromDate,
      toDate,
      search,
      accounts,
      categories,
    ],
  );

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const visible = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  async function confirmVoid(transactionId: string) {
    setError("");
    setVoidingId(transactionId);

    try {
      await onVoidTransaction(transactionId);
      setConfirmId(null);
      setMenuId(null);
    } catch {
      setError("Could not void this transaction. Please try again.");
    } finally {
      setVoidingId(null);
    }
  }

  function resetPageAnd<T>(setter: (value: T) => void, value: T) {
    setter(value);
    setPage(1);
  }

  function clearFilters() {
    setSearch("");
    setTypeFilter("ALL");
    setAccountFilter("ALL");
    setCategoryFilter("ALL");
    setStatusFilter("POSTED");
    setShowVoided(false);
    setFromDate("");
    setToDate("");
    setPage(1);
  }

  return (
    <section className="transactions-page">
      <header className="transactions-toolbar">
        <div>
          <h2>Transactions</h2>
          <p>Search, filter, and manage your money movement.</p>
        </div>

        <button
          type="button"
          className="add-transaction-trigger"
          onClick={onAddTransaction}
        >
          ＋ Add transaction
        </button>
      </header>

      {error && (
        <div className="transactions-error" role="alert">
          <CircleAlert size={17} />
          {error}
          <button
            type="button"
            onClick={() => setError("")}
            aria-label="Dismiss error"
          >
            <X size={16} />
          </button>
        </div>
      )}

      <div className="transaction-filters">
        <label className="transaction-search">
          <Search size={17} />
          <input
            aria-label="Search transactions"
            placeholder="Search description, account, category…"
            value={search}
            onChange={(event) => resetPageAnd(setSearch, event.target.value)}
          />
        </label>

        <label>
          <span>Type</span>
          <select
            value={typeFilter}
            onChange={(event) =>
              resetPageAnd(setTypeFilter, event.target.value)
            }
          >
            <option value="ALL">All types</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
            <option value="TRANSFER">Transfer</option>
          </select>
        </label>

        <label>
          <span>Account</span>
          <select
            value={accountFilter}
            onChange={(event) =>
              resetPageAnd(setAccountFilter, event.target.value)
            }
          >
            <option value="ALL">All accounts</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Category</span>
          <select
            value={categoryFilter}
            onChange={(event) =>
              resetPageAnd(setCategoryFilter, event.target.value)
            }
          >
            <option value="ALL">All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>From</span>
          <input
            type="date"
            value={fromDate}
            onChange={(event) => resetPageAnd(setFromDate, event.target.value)}
          />
        </label>

        <label>
          <span>To</span>
          <input
            type="date"
            min={fromDate || undefined}
            value={toDate}
            onChange={(event) => resetPageAnd(setToDate, event.target.value)}
          />
        </label>

        <label>
          <span>Status</span>
          <select
            value={statusFilter}
            onChange={(event) => {
              resetPageAnd(setStatusFilter, event.target.value);

              if (
                event.target.value === "VOIDED" ||
                event.target.value === "ALL"
              ) {
                setShowVoided(true);
              }
            }}
          >
            <option value="POSTED">Posted</option>
            <option value="VOIDED">Voided</option>
            <option value="ALL">All statuses</option>
          </select>
        </label>

        <label className="transaction-show-voided">
          <input
            type="checkbox"
            checked={showVoided}
            onChange={(event) => {
              setShowVoided(event.target.checked);
              setStatusFilter(event.target.checked ? "ALL" : "POSTED");
              setPage(1);
            }}
          />
          Show voided
        </label>
      </div>

      <div className="transactions-result-count">
        {filtered.length}{" "}
        {filtered.length === 1 ? "transaction" : "transactions"}
      </div>

      {visible.length === 0 ? (
        <div className="transactions-empty-state">
          <div className="transactions-empty-icon">
            <Wallet size={26} />
          </div>

          <h3>No matching transactions</h3>
          <p>Try changing your filters or add a transaction.</p>

          <button
            type="button"
            className="secondary-button"
            onClick={clearFilters}
          >
            Clear filters
          </button>
        </div>
      ) : (
        <>
          <div className="transactions-table-wrap">
            <table className="transactions-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Category</th>
                  <th>Account</th>
                  <th className="amount-heading">Amount</th>
                  <th>
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>

              <tbody>
                {visible.map((transaction) => {
                  const account = getAccountById(
                    accounts,
                    transaction.accountId,
                  );
                  const isVoided = transaction.status === "VOIDED";

                  return (
                    <tr
                      key={transaction.id}
                      className={isVoided ? "is-voided" : ""}
                    >
                      <td data-label="Date">
                        <span className="desktop-date">
                          {formatDate(transaction.occurredAt)}
                        </span>
                        <span className="mobile-date">
                          {new Date(transaction.occurredAt).toLocaleDateString(
                            "en-IN",
                            {
                              day: "2-digit",
                              month: "short",
                              year: "numeric",
                            },
                          )}
                        </span>
                      </td>

                      <td data-label="Description">
                        <div className="transaction-description-cell">
                          <span
                            className={`transaction-type-icon transaction-type-${transaction.type.toLowerCase()}`}
                          >
                            {transaction.type === "INCOME" ? (
                              <ArrowDownLeft size={16} />
                            ) : transaction.type === "EXPENSE" ? (
                              <ArrowUpRight size={16} />
                            ) : (
                              <ArrowLeftRight size={16} />
                            )}
                          </span>

                          <div>
                            <strong>{getTransactionTitle(transaction)}</strong>
                            <small>
                              {transaction.type === "TRANSFER"
                                ? getTransferLabel(transaction, accounts)
                                : formatTypeLabel(transaction.type)}

                              {isVoided && (
                                <span className="transaction-voided-pill">
                                  Voided
                                </span>
                              )}
                            </small>
                          </div>
                        </div>
                      </td>

                      <td data-label="Category">
                        {transaction.type === "TRANSFER"
                          ? "—"
                          : getCategoryNameById(
                              categories,
                              transaction.categoryId,
                            )}
                      </td>

                      <td data-label="Account">
                        {transaction.type === "TRANSFER"
                          ? getTransferLabel(transaction, accounts)
                          : getAccountNameById(accounts, transaction.accountId)}
                      </td>

                      <td
                        data-label="Amount"
                        className={`transaction-table-amount ${transaction.type.toLowerCase()}`}
                      >
                        {transaction.type === "INCOME"
                          ? "+"
                          : transaction.type === "EXPENSE"
                            ? "−"
                            : ""}
                        {formatMoney(transaction.amount, account?.currency)}
                      </td>

                      <td className="transaction-actions-cell">
                        <div className="transaction-menu-wrap">
                          <button
                            type="button"
                            className="transaction-menu-trigger"
                            aria-label={`Actions for ${getTransactionTitle(transaction)}`}
                            aria-expanded={menuId === transaction.id}
                            onClick={() =>
                              setMenuId(
                                menuId === transaction.id
                                  ? null
                                  : transaction.id,
                              )
                            }
                          >
                            <MoreHorizontal size={19} />
                          </button>

                          {menuId === transaction.id && (
                            <div className="transaction-row-menu">
                              <button
                                type="button"
                                onClick={() => {
                                  onDuplicateTransaction(transaction);
                                  setMenuId(null);
                                }}
                              >
                                <Copy size={15} />
                                Duplicate as new
                              </button>

                              {!isVoided && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    setConfirmId(transaction.id);
                                    setMenuId(null);
                                  }}
                                >
                                  <Ban size={15} />
                                  Void transaction
                                </button>
                              )}
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {confirmId && (
            <div className="transaction-confirm-overlay" role="presentation">
              <section
                className="transaction-confirm-dialog"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="void-title"
              >
                <h3 id="void-title">Void this transaction?</h3>
                <p>
                  Voiding changes the transaction status and may affect account
                  balances and analytics. This cannot be undone from this
                  screen.
                </p>

                <div>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => setConfirmId(null)}
                    disabled={voidingId !== null}
                  >
                    Keep transaction
                  </button>

                  <button
                    type="button"
                    className="danger-button"
                    onClick={() => void confirmVoid(confirmId)}
                    disabled={voidingId !== null}
                  >
                    {voidingId === confirmId ? "Voiding…" : "Confirm void"}
                  </button>
                </div>
              </section>
            </div>
          )}

          <footer className="transactions-pagination">
            <span>
              Showing {(page - 1) * PAGE_SIZE + 1}–
              {Math.min(page * PAGE_SIZE, filtered.length)} of {filtered.length}
            </span>

            <div>
              <button
                type="button"
                className="secondary-button"
                disabled={page <= 1}
                onClick={() => setPage((current) => current - 1)}
              >
                Previous
              </button>

              <span>
                Page {page} of {pageCount}
              </span>

              <button
                type="button"
                className="secondary-button"
                disabled={page >= pageCount}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
              </button>
            </div>
          </footer>
        </>
      )}
    </section>
  );
}

export default TransactionList;
