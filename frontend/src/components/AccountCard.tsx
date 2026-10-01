import { useState } from "react";
import type { AccountType } from "../types/account";

type AccountCardProps = {
  name: string;
  type: AccountType;
  currency: string;
  openingBalance: number;
  currentBalance: number;
  onRemove: () => Promise<void>;
  onUpdate: (
    name: string,
    type: AccountType,
    currency: string,
  ) => Promise<void>;
};

export default function AccountCard({
  name,
  type,
  currency,
  currentBalance,
  onRemove,
  onUpdate,
}: AccountCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editedName, setEditedName] = useState(name);
  const [editedType, setEditedType] = useState<AccountType>(type);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);

  async function handleDelete() {
    if (!window.confirm(`Delete account "${name}"?`)) {
      return;
    }

    try {
      setIsDeleting(true);
      await onRemove();
    } finally {
      setIsDeleting(false);
    }
  }

  async function handleUpdate() {
    const trimmedName = editedName.trim();

    if (!trimmedName) {
      return;
    }

    try {
      setIsUpdating(true);
      await onUpdate(trimmedName, editedType, currency);
      setIsEditing(false);
    } finally {
      setIsUpdating(false);
    }
  }

  function handleCancelEdit() {
    setEditedName(name);
    setEditedType(type);
    setIsEditing(false);
  }

  return (
    <div>
      {isEditing ? (
        <div>
          <div>
            <label>
              Name
              <input
                value={editedName}
                onChange={(event) => setEditedName(event.target.value)}
                disabled={isUpdating}
              />
            </label>
          </div>

          <div>
            <label>
              Type
              <select
                value={editedType}
                onChange={(event) =>
                  setEditedType(event.target.value as AccountType)
                }
                disabled={isUpdating}
              >
                <option value="BANK">Bank</option>
                <option value="CASH">Cash</option>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="INVESTMENT">Investment</option>
                <option value="OTHER">Other</option>
              </select>
            </label>
          </div>

          <p>
            Current balance: {currency} {currentBalance}
          </p>

          <button onClick={handleUpdate} disabled={isUpdating}>
            {isUpdating ? "Saving..." : "Save"}
          </button>

          <button onClick={handleCancelEdit} disabled={isUpdating}>
            Cancel
          </button>
        </div>
      ) : (
        <div>
          <h3>{name}</h3>

          <p>Type: {type}</p>

          <p>
            Current balance: {currency} {currentBalance}
          </p>

          <button onClick={() => setIsEditing(true)} disabled={isDeleting}>
            Edit
          </button>

          <button onClick={handleDelete} disabled={isDeleting}>
            {isDeleting ? "Deleting..." : "Delete"}
          </button>
        </div>
      )}
    </div>
  );
}
