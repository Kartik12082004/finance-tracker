import { useState } from "react";
import type { Account } from "../types/account";
import type { Category } from "../types/category";
import type { Transaction } from "../types/transaction";

type TransactionListProps = {
  transactions: Transaction[];
  accounts: Account[];
  categories: Category[];
  onVoidTransaction: (transactionId: string) => Promise<void>;
};

function TransactionList({
  transactions,
  accounts,
  categories,
  onVoidTransaction,
}: TransactionListProps) {
  const [voidingTransactionId, setVoidingTransactionId] = useState<
    string | null
  >(null);
  const [error, setError] = useState("");

  function getAccountName(accountId: string): string {
    return (
      accounts.find((account) => account.id === accountId)?.name ??
      "Unknown account"
    );
  }

  function getCategoryName(categoryId: string | null): string {
    if (categoryId === null) {
      return "-";
    }

    return (
      categories.find((category) => category.id === categoryId)?.name ??
      "Unknown category"
    );
  }

  function formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }

  async function handleVoid(transactionId: string) {
    setError("");
    setVoidingTransactionId(transactionId);

    try {
      await onVoidTransaction(transactionId);
    } catch {
      setError("Transaction could not be voided.");
    } finally {
      setVoidingTransactionId(null);
    }
  }

  if (transactions.length === 0) {
    return <p>No transactions yet.</p>;
  }

  return (
    <div>
      <h2>Transactions</h2>

      {error && <p>{error}</p>}

      {transactions.map((transaction) => (
        <div key={transaction.id}>
          <p>
            <strong>{transaction.type}</strong>
          </p>

          <p>Status: {transaction.status}</p>

          <p>Amount: {transaction.amount}</p>

          <p>Account: {getAccountName(transaction.accountId)}</p>

          {transaction.type === "TRANSFER" ? (
            <p>
              Destination:{" "}
              {transaction.destinationAccountId
                ? getAccountName(transaction.destinationAccountId)
                : "Unknown account"}
            </p>
          ) : (
            <p>Category: {getCategoryName(transaction.categoryId)}</p>
          )}

          <p>Description: {transaction.description ?? "-"}</p>

          <p>Occurred: {formatDate(transaction.occurredAt)}</p>

          {transaction.status === "POSTED" && (
            <button
              type="button"
              onClick={() => handleVoid(transaction.id)}
              disabled={voidingTransactionId !== null}
            >
              {voidingTransactionId === transaction.id ? "Voiding..." : "Void"}
            </button>
          )}

          <hr />
        </div>
      ))}
    </div>
  );
}

export default TransactionList;
