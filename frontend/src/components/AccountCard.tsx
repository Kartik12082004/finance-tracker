import { useEffect, useRef, useState } from "react";
import type { Account, AccountType } from "../types/account";
import { CreditCard, Landmark, Wallet, Trash2 } from "lucide-react";

type AccountCardProps = {
  name: string;
  type: AccountType;
  currency: string;
  currentBalance: number;
  creditLimit: number | null;
  availableCredit: number | null;
  parentAccountId: string | null;
  bankAccounts: Account[];
  onRemove: () => Promise<void>;
  onUpdate: (
    name: string,
    type: AccountType,
    currency: string,
    creditLimit: number | null,
    parentAccountId: string | null,
  ) => Promise<void>;
};

function money(currency: string, amount: number): string {
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${currency} ${amount.toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  }
}

function formatAccountType(type: AccountType): string {
  return type.replaceAll("_", " ");
}

export default function AccountCard({
  name,
  type,
  currency,
  currentBalance,
  creditLimit,
  availableCredit,
  parentAccountId,
  bankAccounts,
  onRemove,
  onUpdate,
}: AccountCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editedName, setEditedName] = useState(name);
  const [editedCreditLimit, setEditedCreditLimit] = useState(
    creditLimit?.toString() ?? "",
  );
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [error, setError] = useState("");

  const deleteCancelButtonRef = useRef<HTMLButtonElement>(null);
  const deleteTriggerButtonRef = useRef<HTMLButtonElement>(null);

  const isCreditCard = type === "CREDIT_CARD";
  const linkedBank = bankAccounts.find(
    (account) => account.id === parentAccountId,
  );

  const outstandingDebt = Math.max(currentBalance, 0);
  const utilization =
    isCreditCard && creditLimit && creditLimit > 0
      ? Math.min((outstandingDebt / creditLimit) * 100, 100)
      : 0;

  useEffect(() => {
    if (!showDeleteConfirmation) return;

    deleteCancelButtonRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !isDeleting) {
        event.preventDefault();
        setShowDeleteConfirmation(false);
        setError("");
        requestAnimationFrame(() => {
          deleteTriggerButtonRef.current?.focus();
        });
      }
    }

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [showDeleteConfirmation, isDeleting]);

  const startEditing = () => {
    setEditedName(name);
    setEditedCreditLimit(creditLimit?.toString() ?? "");
    setError("");
    setIsEditing(true);
    setShowDeleteConfirmation(false);
  };

  const handleCancelEdit = () => {
    setEditedName(name);
    setEditedCreditLimit(creditLimit?.toString() ?? "");
    setError("");
    setIsEditing(false);
  };

  const handleUpdate = async () => {
    const trimmedName = editedName.trim();

    if (!trimmedName) {
      setError("Account name cannot be empty.");
      return;
    }

    let parsedCreditLimit: number | null = null;

    if (isCreditCard) {
      const normalizedLimit = editedCreditLimit.trim();

      if (!/^\d+(\.\d{1,2})?$/.test(normalizedLimit)) {
        setError("Enter a valid credit limit with up to two decimal places.");
        return;
      }

      parsedCreditLimit = Number(normalizedLimit);

      if (!Number.isFinite(parsedCreditLimit) || parsedCreditLimit <= 0) {
        setError("Credit limit must be greater than zero.");
        return;
      }
    }

    setIsUpdating(true);
    setError("");

    try {
      await onUpdate(
        trimmedName,
        type,
        currency,
        isCreditCard ? parsedCreditLimit : null,
        parentAccountId,
      );

      setIsEditing(false);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to update account.",
      );
    } finally {
      setIsUpdating(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    setError("");

    try {
      await onRemove();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to delete account.",
      );
      setShowDeleteConfirmation(false);
      requestAnimationFrame(() => {
        deleteTriggerButtonRef.current?.focus();
      });
    } finally {
      setIsDeleting(false);
    }
  };

  const cancelDelete = () => {
    if (isDeleting) return;

    setShowDeleteConfirmation(false);
    setError("");

    requestAnimationFrame(() => {
      deleteTriggerButtonRef.current?.focus();
    });
  };

  return (
    <article className="account-card">
      <div className="account-card-heading">
        <div className="account-card-identity">
          <div className="account-card-type-icon" aria-hidden="true">
            {type === "CREDIT_CARD" ? (
              <CreditCard size={21} strokeWidth={1.8} />
            ) : type === "BANK" ? (
              <Landmark size={21} strokeWidth={1.8} />
            ) : (
              <Wallet size={21} strokeWidth={1.8} />
            )}
          </div>

          <div className="account-card-title-group">
            <h3 className="account-card-name">{name}</h3>
            <div className="account-card-meta">
              <span className="account-type-label">
                {formatAccountType(type)}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="account-card-balance">
        <span className="account-balance-label">
          {isCreditCard
            ? currentBalance > 0
              ? "Outstanding debt"
              : currentBalance < 0
                ? "Credit balance"
                : "Outstanding debt"
            : "Current balance"}
        </span>

        <strong
          className={
            isCreditCard && currentBalance > 0
              ? "account-debt-value"
              : "account-balance-value"
          }
        >
          {money(currency, Math.abs(currentBalance))}
        </strong>
      </div>

      {isCreditCard && (
        <div className="account-credit-details">
          <div className="account-credit-row">
            <span>Credit limit</span>
            <strong>
              {creditLimit !== null ? money(currency, creditLimit) : "Not set"}
            </strong>
          </div>

          <div className="account-credit-row">
            <span>Available credit</span>
            <strong>
              {availableCredit !== null
                ? money(currency, availableCredit)
                : "Not available"}
            </strong>
          </div>

          <div className="account-utilization">
            <div className="account-utilization-heading">
              <span>Credit utilization</span>
              <strong>{utilization.toFixed(1)}%</strong>
            </div>

            <div
              className="account-utilization-track"
              role="progressbar"
              aria-label="Credit utilization"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={Math.round(utilization)}
            >
              <div
                className={`account-utilization-fill ${
                  utilization > 70
                    ? "is-high"
                    : utilization > 30
                      ? "is-medium"
                      : ""
                }`}
                style={{ width: `${utilization}%` }}
              />
            </div>
          </div>

          <div className="account-credit-row">
            <span>Linked bank</span>
            <strong>{linkedBank?.name ?? "Linked bank unavailable"}</strong>
          </div>

          {currentBalance < 0 && (
            <p className="account-card-note">
              Your card has a credit balance of{" "}
              {money(currency, Math.abs(currentBalance))}.
            </p>
          )}
        </div>
      )}

      {isEditing ? (
        <div className="account-card-edit">
          <div className="account-form">
            <label htmlFor={`account-name-${name}`}>
              Account name
              <input
                id={`account-name-${name}`}
                type="text"
                value={editedName}
                onChange={(event) => setEditedName(event.target.value)}
                maxLength={100}
                disabled={isUpdating}
                autoComplete="off"
              />
            </label>

            {isCreditCard && (
              <>
                <label htmlFor={`credit-limit-${name}`}>
                  Credit limit ({currency})
                  <input
                    id={`credit-limit-${name}`}
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={editedCreditLimit}
                    onChange={(event) =>
                      setEditedCreditLimit(event.target.value)
                    }
                    disabled={isUpdating}
                  />
                </label>

                <p className="field-hint">
                  The limit must be positive. Your outstanding debt is not
                  changed by updating it.
                </p>
              </>
            )}

            <div className="account-locked-details">
              <span>
                Account type: <strong>{formatAccountType(type)}</strong>
              </span>
              <span>
                Currency: <strong>{currency}</strong>
              </span>

              {isCreditCard && (
                <span>
                  Linked bank:{" "}
                  <strong>{linkedBank?.name ?? "Unavailable"}</strong>
                </span>
              )}
            </div>
          </div>

          {error && (
            <p className="account-form-error" role="alert">
              {error}
            </p>
          )}

          <div className="account-card-actions">
            <button
              type="button"
              className="secondary-button"
              onClick={handleCancelEdit}
              disabled={isUpdating}
            >
              Cancel
            </button>
            <button
              type="button"
              className="primary-button"
              onClick={handleUpdate}
              disabled={isUpdating}
            >
              {isUpdating ? "Saving..." : "Save changes"}
            </button>
          </div>
        </div>
      ) : (
        <>
          {error && (
            <p className="account-form-error" role="alert">
              {error}
            </p>
          )}

          {showDeleteConfirmation ? (
            <div
              className="account-delete-confirmation"
              role="group"
              aria-label={`Delete ${name}`}
            >
              <div className="account-delete-confirmation-copy">
                <strong>Delete this account?</strong>
                <p>
                  “{name}” will be permanently deleted. This action cannot be
                  undone.
                </p>
              </div>

              <div className="account-card-actions">
                <button
                  ref={deleteCancelButtonRef}
                  type="button"
                  className="secondary-button"
                  onClick={cancelDelete}
                  disabled={isDeleting}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="primary-button account-delete-button"
                  onClick={handleDelete}
                  disabled={isDeleting}
                >
                  <Trash2 size={16} aria-hidden="true" />
                  {isDeleting ? "Deleting..." : "Delete account"}
                </button>
              </div>
            </div>
          ) : (
            <div className="account-card-footer">
              <div className="account-card-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={startEditing}
                  disabled={isDeleting}
                >
                  Edit
                </button>
                <button
                  ref={deleteTriggerButtonRef}
                  type="button"
                  className="secondary-button account-delete-button"
                  onClick={() => {
                    setError("");
                    setShowDeleteConfirmation(true);
                  }}
                  disabled={isDeleting}
                  aria-label={`Delete account ${name}`}
                >
                  <Trash2 size={15} aria-hidden="true" />
                  Delete
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </article>
  );
}
