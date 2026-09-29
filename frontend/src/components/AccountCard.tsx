import { useState } from "react";
import type { AccountType } from "../types/account";

type AccountCardProps = {
  name: string;
  type: string;
  currency: string;
  openingBalance: number;
  onRemove: () => Promise<void>;
  onUpdate: (
    name: string,
    type: AccountType,
    currency: string,
  ) => Promise<void>;
};

function AccountCard({
  name,
  type,
  currency,
  openingBalance,
  onRemove,
  onUpdate,
}: AccountCardProps) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);

  const [editName, setEditName] = useState(name);
  const [editType, setEditType] = useState<AccountType>(type as AccountType);
  const [editCurrency, setEditCurrency] = useState(currency);

  const [error, setError] = useState("");

  async function handleDelete() {
    setError("");
    setIsDeleting(true);

    try {
      await onRemove();
    } catch {
      setError(
        "Account could not be deleted. It may contain financial transactions.",
      );
    } finally {
      setIsDeleting(false);
    }
  }

  function handleEdit() {
    setError("");

    // Start editing with the account's current values.
    setEditName(name);
    setEditType(type as AccountType);
    setEditCurrency(currency);

    setIsEditing(true);
  }

  function handleCancelEdit() {
    setError("");

    // Restore the original values when cancelling.
    setEditName(name);
    setEditType(type as AccountType);
    setEditCurrency(currency);

    setIsEditing(false);
  }

  async function handleUpdate() {
    setError("");

    if (editName.trim() === "") {
      setError("Account name is required.");
      return;
    }

    if (editCurrency.trim().length !== 3) {
      setError("Currency must be a 3-letter code.");
      return;
    }

    setIsUpdating(true);

    try {
      await onUpdate(
        editName.trim(),
        editType,
        editCurrency.trim().toUpperCase(),
      );

      setIsEditing(false);
    } catch {
      setError("Failed to update account.");
    } finally {
      setIsUpdating(false);
    }
  }

  if (isEditing) {
    return (
      <div>
        <h2>Edit Account</h2>

        <label>
          Account name:
          <input
            type="text"
            value={editName}
            onChange={(event) => setEditName(event.target.value)}
          />
        </label>

        <br />

        <label>
          Account type:
          <select
            value={editType}
            onChange={(event) => setEditType(event.target.value as AccountType)}
          >
            <option value="BANK">Bank</option>
            <option value="CASH">Cash</option>
            <option value="CREDIT_CARD">Credit Card</option>
            <option value="INVESTMENT">Investment</option>
          </select>
        </label>

        <br />

        <label>
          Currency:
          <input
            type="text"
            maxLength={3}
            value={editCurrency}
            onChange={(event) => setEditCurrency(event.target.value)}
          />
        </label>

        <br />

        <p>
          Opening balance: {currency} {openingBalance}
        </p>

        <p>Opening balance cannot be changed when editing an account.</p>

        {error && <p>{error}</p>}

        <button type="button" onClick={handleUpdate} disabled={isUpdating}>
          {isUpdating ? "Saving..." : "Save"}
        </button>

        <button type="button" onClick={handleCancelEdit} disabled={isUpdating}>
          Cancel
        </button>
      </div>
    );
  }

  return (
    <div>
      <h2>{name}</h2>

      <p>Type: {type}</p>

      <p>
        Opening balance: {currency} {openingBalance}
      </p>

      {error && <p>{error}</p>}

      <button type="button" onClick={handleEdit} disabled={isDeleting}>
        Edit
      </button>

      <button type="button" onClick={handleDelete} disabled={isDeleting}>
        {isDeleting ? "Deleting..." : "Delete"}
      </button>
    </div>
  );
}

export default AccountCard;
