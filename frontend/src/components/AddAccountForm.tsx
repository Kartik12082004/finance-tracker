import { useCallback, useEffect, useRef, useState } from "react";

import type { Account, AccountType } from "../types/account";

type AddAccountFormProps = {
  accounts: Account[];
  onAddAccount: (
    name: string,
    type: AccountType,
    currency: string,
    openingBalance: number,
    creditLimit: number | null,
    parentAccountId: string | null,
  ) => Promise<void>;
  onOpen?: (open: () => void) => void;
};

const currencies = [
  { code: "INR", label: "INR — Indian Rupee" },
  { code: "USD", label: "USD — US Dollar" },
  { code: "EUR", label: "EUR — Euro" },
  { code: "GBP", label: "GBP — British Pound" },
];

function AddAccountForm({
  accounts,
  onAddAccount,
  onOpen,
}: AddAccountFormProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [name, setName] = useState("");
  const [type, setType] = useState<AccountType>("BANK");
  const [currency, setCurrency] = useState("INR");
  const [openingBalance, setOpeningBalance] = useState("0");
  const [creditLimit, setCreditLimit] = useState("");
  const [parentAccountId, setParentAccountId] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState("");

  const dialogRef = useRef<HTMLElement>(null);
  const nameInputRef = useRef<HTMLInputElement>(null);
  const openerRef = useRef<HTMLElement | null>(null);

  const bankAccounts = accounts.filter((account) => account.type === "BANK");

  const isCreditCard = type === "CREDIT_CARD";

  const openModal = useCallback(() => {
    openerRef.current =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;

    setSuccess("");
    setError("");
    setIsOpen(true);
  }, []);

  useEffect(() => {
    onOpen?.(openModal);
  }, [onOpen, openModal]);

  useEffect(() => {
    if (!isOpen) return;

    nameInputRef.current?.focus();

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
          'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])',
        ),
      ).filter((element) => element.getAttribute("aria-hidden") !== "true");

      if (focusableElements.length === 0) {
        event.preventDefault();
        dialog.focus();
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];
      const activeElement = document.activeElement;

      if (event.shiftKey && activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      } else if (!event.shiftKey && activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      } else if (!dialog.contains(activeElement)) {
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

  function resetForm() {
    setName("");
    setType("BANK");
    setCurrency("INR");
    setOpeningBalance("0");
    setCreditLimit("");
    setParentAccountId("");
    setError("");
  }

  function closeModal() {
    if (isSubmitting) return;

    setIsOpen(false);
    setError("");
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) return;

    setError("");
    setSuccess("");

    if (!name.trim()) {
      setError("Account name is required.");
      nameInputRef.current?.focus();
      return;
    }

    const parsedOpeningBalance = Number(openingBalance);

    if (
      openingBalance.trim() === "" ||
      !Number.isFinite(parsedOpeningBalance) ||
      parsedOpeningBalance < 0 ||
      !/^\d+(\.\d{1,2})?$/.test(openingBalance.trim())
    ) {
      setError(
        "Opening balance must be zero or greater, with at most 2 decimal places.",
      );
      return;
    }

    let parsedCreditLimit: number | null = null;
    let linkedBankId: string | null = null;

    if (isCreditCard) {
      parsedCreditLimit = Number(creditLimit);

      if (
        creditLimit.trim() === "" ||
        !Number.isFinite(parsedCreditLimit) ||
        parsedCreditLimit <= 0 ||
        !/^\d+(\.\d{1,2})?$/.test(creditLimit.trim())
      ) {
        setError(
          "Credit limit must be greater than zero, with at most 2 decimal places.",
        );
        return;
      }

      const selectedBank = bankAccounts.find(
        (account) => account.id === parentAccountId,
      );

      if (!selectedBank) {
        setError("Select a linked bank account for this credit card.");
        return;
      }

      if (selectedBank.currency !== currency) {
        setError("The credit card and linked bank must use the same currency.");
        return;
      }

      linkedBankId = selectedBank.id;
    }

    setIsSubmitting(true);

    try {
      await onAddAccount(
        name.trim(),
        type,
        currency,
        parsedOpeningBalance,
        parsedCreditLimit,
        linkedBankId,
      );

      resetForm();
      setIsOpen(false);
      setSuccess("Account added successfully.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add account.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      {success && (
        <p className="account-success-message" role="status" aria-live="polite">
          {success}
        </p>
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
            className="account-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-account-title"
            aria-describedby="add-account-description"
            tabIndex={-1}
          >
            <div className="account-modal-header">
              <div>
                <p className="eyebrow">FINANCIAL ACCOUNTS</p>
                <h2 id="add-account-title">Add account</h2>
                <p
                  id="add-account-description"
                  className="account-modal-description"
                >
                  Add an account to keep your finances organized.
                </p>
              </div>

              <button
                type="button"
                className="icon-button"
                onClick={closeModal}
                disabled={isSubmitting}
                aria-label="Close add account dialog"
              >
                ×
              </button>
            </div>

            <form className="account-form" onSubmit={handleSubmit}>
              <label>
                Account name
                <input
                  ref={nameInputRef}
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="e.g. ICICI Savings"
                  maxLength={100}
                  disabled={isSubmitting}
                  autoComplete="off"
                  required
                  aria-invalid={Boolean(error && !name.trim())}
                />
              </label>

              <label>
                Account type
                <select
                  value={type}
                  onChange={(event) => {
                    const nextType = event.target.value as AccountType;

                    setType(nextType);
                    setError("");

                    if (nextType !== "CREDIT_CARD") {
                      setCreditLimit("");
                      setParentAccountId("");
                    }
                  }}
                  disabled={isSubmitting}
                >
                  <option value="BANK">Bank account</option>
                  <option value="CASH">Cash</option>
                  <option value="CREDIT_CARD">Credit card</option>
                  <option value="INVESTMENT">Investment</option>
                </select>
              </label>

              <label>
                Currency
                <select
                  value={currency}
                  onChange={(event) => {
                    setCurrency(event.target.value);
                    setError("");
                  }}
                  disabled={isSubmitting}
                >
                  {currencies.map((item) => (
                    <option key={item.code} value={item.code}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                {isCreditCard ? "Current amount owed" : "Opening balance"}

                <div className="account-money-input">
                  <span aria-hidden="true">
                    {currency === "INR"
                      ? "₹"
                      : currency === "USD"
                        ? "$"
                        : currency === "EUR"
                          ? "€"
                          : "£"}
                  </span>

                  <input
                    type="number"
                    inputMode="decimal"
                    min="0"
                    step="0.01"
                    value={openingBalance}
                    onChange={(event) => setOpeningBalance(event.target.value)}
                    disabled={isSubmitting}
                    required
                    aria-describedby="opening-balance-hint"
                  />
                </div>

                <span id="opening-balance-hint" className="field-hint">
                  {isCreditCard
                    ? "Enter existing card debt. Use 0 for a new card with no outstanding balance."
                    : "Enter the money currently held in this account."}
                </span>
              </label>

              {isCreditCard && (
                <div className="account-credit-card-fields">
                  <label>
                    Credit limit
                    <div className="account-money-input">
                      <span aria-hidden="true">
                        {currency === "INR"
                          ? "₹"
                          : currency === "USD"
                            ? "$"
                            : currency === "EUR"
                              ? "€"
                              : "£"}
                      </span>

                      <input
                        type="number"
                        inputMode="decimal"
                        min="0.01"
                        step="0.01"
                        value={creditLimit}
                        onChange={(event) => setCreditLimit(event.target.value)}
                        disabled={isSubmitting}
                        required
                      />
                    </div>
                  </label>

                  <label>
                    Linked bank account
                    <select
                      value={parentAccountId}
                      onChange={(event) =>
                        setParentAccountId(event.target.value)
                      }
                      disabled={isSubmitting || bankAccounts.length === 0}
                      required
                    >
                      <option value="">Select a bank account</option>

                      {bankAccounts
                        .filter((account) => account.currency === currency)
                        .map((account) => (
                          <option key={account.id} value={account.id}>
                            {account.name} ({account.currency})
                          </option>
                        ))}
                    </select>
                  </label>

                  {bankAccounts.length === 0 && (
                    <p className="field-hint" role="status">
                      Create a bank account first, then link your credit card.
                    </p>
                  )}

                  {bankAccounts.length > 0 &&
                    !bankAccounts.some(
                      (account) => account.currency === currency,
                    ) && (
                      <p className="field-hint" role="status">
                        No bank account uses this currency. Select a matching
                        currency or create a bank account first.
                      </p>
                    )}
                </div>
              )}

              {error && (
                <p
                  className="account-form-error"
                  role="alert"
                  aria-live="assertive"
                >
                  {error}
                </p>
              )}

              <div className="account-modal-actions">
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
                  disabled={
                    isSubmitting ||
                    (isCreditCard &&
                      !bankAccounts.some(
                        (account) =>
                          account.id === parentAccountId &&
                          account.currency === currency,
                      ))
                  }
                  aria-busy={isSubmitting}
                >
                  {isSubmitting ? "Adding account..." : "Add account"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </>
  );
}

export default AddAccountForm;
