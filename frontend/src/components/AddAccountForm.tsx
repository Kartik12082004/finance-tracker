import { useState } from "react";
import type { AccountType } from "../types/account";

type AddAccountFormProps = {
  onAddAccount: (
    name: string,
    type: AccountType,
    currency: string,
    openingBalance: number,
  ) => Promise<void>;
};

function AddAccountForm({ onAddAccount }: AddAccountFormProps) {
  const [name, setName] = useState("");
  const [type, setType] = useState<AccountType>("BANK");
  const [currency, setCurrency] = useState("INR");
  const [openingBalance, setOpeningBalance] = useState("0");

  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState("");

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError("");
    setSuccess("");

    if (name.trim() === "") {
      setError("Account name is required.");
      return;
    }

    if (currency.trim().length !== 3) {
      setError("Currency must be a 3-letter code.");
      return;
    }

    const parsedOpeningBalance = Number(openingBalance);

    if (
      openingBalance.trim() === "" ||
      Number.isNaN(parsedOpeningBalance) ||
      parsedOpeningBalance < 0
    ) {
      setError("Opening balance must be zero or greater.");
      return;
    }

    setIsSubmitting(true);

    try {
      await onAddAccount(
        name.trim(),
        type,
        currency.trim().toUpperCase(),
        parsedOpeningBalance,
      );

      setName("");
      setType("BANK");
      setCurrency("INR");
      setOpeningBalance("0");
      setSuccess("Account added successfully.");
    } catch {
      setError("Failed to add account.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>Add Account</h2>

      <label>
        Account name:
        <input
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
        />
      </label>

      <br />

      <label>
        Account type:
        <select
          value={type}
          onChange={(event) => setType(event.target.value as AccountType)}
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
          value={currency}
          onChange={(event) => setCurrency(event.target.value)}
        />
      </label>

      <br />

      <label>
        Opening balance:
        <input
          type="number"
          min="0"
          step="0.01"
          value={openingBalance}
          onChange={(event) => setOpeningBalance(event.target.value)}
        />
      </label>

      <br />

      {error && <p>{error}</p>}
      {success && <p>{success}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Adding..." : "Add Account"}
      </button>
    </form>
  );
}

export default AddAccountForm;
