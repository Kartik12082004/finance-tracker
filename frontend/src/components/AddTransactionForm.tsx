import { useState } from "react";
import { createTransaction } from "../api/transactionApi";
import type { Account } from "../types/account";
import type { Category } from "../types/category";
import type {
  CreateTransactionRequest,
  TransactionType,
} from "../types/transaction";

type AddTransactionFormProps = {
  accounts: Account[];
  categories: Category[];
  onTransactionCreated: () => Promise<void>;
};

function AddTransactionForm({
  accounts,
  categories,
  onTransactionCreated,
}: AddTransactionFormProps) {
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [accountId, setAccountId] = useState("");
  const [destinationAccountId, setDestinationAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [occurredAt, setOccurredAt] = useState(
    new Date().toISOString().slice(0, 16),
  );

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // A category is a parent if another category references its ID
  // through parentId. Parent categories are used for grouping and
  // should not be selected directly on a transaction.
  const parentCategoryIds = new Set(
    categories
      .filter((category) => category.parentId !== null)
      .map((category) => category.parentId),
  );

  // Only leaf categories matching the transaction type are valid
  // transaction categories.
  const availableCategories = categories.filter(
    (category) => category.type === type && !parentCategoryIds.has(category.id),
  );

  // A transfer cannot use the same account as both source and destination.
  const destinationAccounts = accounts.filter(
    (account) => account.id !== accountId,
  );

  function handleTypeChange(newType: TransactionType) {
    setType(newType);

    // Category and destination account only apply to their respective
    // transaction types, so clear them when the type changes.
    setCategoryId("");
    setDestinationAccountId("");
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError("");
    setSuccess("");

    const selectedAccountId = accountId || accounts[0]?.id;

    if (!selectedAccountId) {
      setError("Please select an account.");
      return;
    }

    const parsedAmount = Number(amount);

    if (
      amount.trim() === "" ||
      Number.isNaN(parsedAmount) ||
      parsedAmount <= 0
    ) {
      setError("Amount must be greater than zero.");
      return;
    }

    if (type === "TRANSFER" && !destinationAccountId) {
      setError("Please select a destination account.");
      return;
    }

    if (type !== "TRANSFER" && !categoryId) {
      setError("Please select a category.");
      return;
    }

    setIsSubmitting(true);

    try {
      const request: CreateTransactionRequest = {
        accountId: selectedAccountId,
        type,
        amount: parsedAmount,
        occurredAt: new Date(occurredAt).toISOString(),
      };

      if (type === "TRANSFER") {
        request.destinationAccountId = destinationAccountId;
      } else {
        request.categoryId = categoryId;
      }

      if (description.trim() !== "") {
        request.description = description.trim();
      }

      await createTransaction(request);
      await onTransactionCreated();

      setAmount("");
      setDescription("");
      setCategoryId("");
      setDestinationAccountId("");
      setOccurredAt(new Date().toISOString().slice(0, 16));
      setSuccess("Transaction created successfully.");
    } catch {
      setError("Failed to create transaction.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>Add Transaction</h2>

      <label>
        Transaction type:
        <select
          value={type}
          onChange={(event) =>
            handleTypeChange(event.target.value as TransactionType)
          }
        >
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
          <option value="TRANSFER">Transfer</option>
        </select>
      </label>

      <br />

      <label>
        Account:
        <select
          value={accountId}
          onChange={(event) => setAccountId(event.target.value)}
        >
          <option value="">Select account</option>

          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name} ({account.currency})
            </option>
          ))}
        </select>
      </label>

      <br />

      {type === "TRANSFER" ? (
        <>
          <label>
            Destination account:
            <select
              value={destinationAccountId}
              onChange={(event) => setDestinationAccountId(event.target.value)}
            >
              <option value="">Select destination</option>

              {destinationAccounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name} ({account.currency})
                </option>
              ))}
            </select>
          </label>

          <br />
        </>
      ) : (
        <>
          <label>
            Category:
            <select
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
          </label>

          <br />
        </>
      )}

      <label>
        Amount:
        <input
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
        />
      </label>

      <br />

      <label>
        Description:
        <input
          type="text"
          maxLength={255}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
      </label>

      <br />

      <label>
        Date and time:
        <input
          type="datetime-local"
          value={occurredAt}
          onChange={(event) => setOccurredAt(event.target.value)}
        />
      </label>

      <br />

      {error && <p>{error}</p>}
      {success && <p>{success}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Creating..." : "Create Transaction"}
      </button>
    </form>
  );
}

export default AddTransactionForm;
