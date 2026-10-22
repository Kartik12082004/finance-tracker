import { useMemo } from "react";
import { Plus } from "lucide-react";
import AccountCard from "./AccountCard";
import type { Account, AccountType } from "../types/account";
import type { AccountBalance } from "../api/analyticsApi";

type AccountListProps = {
  accounts: Account[];
  accountBalances: AccountBalance[];
  onAddAccount: () => void;
  onRemoveAccount: (id: string) => Promise<void>;
  onUpdateAccount: (
    id: string,
    name: string,
    type: AccountType,
    currency: string,
    creditLimit: number | null,
    parentAccountId: string | null,
  ) => Promise<void>;
};

function money(amount: number, currency: string): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

function AccountList({
  accounts,
  accountBalances,
  onAddAccount,
  onRemoveAccount,
  onUpdateAccount,
}: AccountListProps) {
  const balancesByAccountId = useMemo(
    () =>
      new Map(accountBalances.map((balance) => [balance.accountId, balance])),
    [accountBalances],
  );

  const bankAccounts = useMemo(
    () => accounts.filter((account) => account.type === "BANK"),
    [accounts],
  );

  const summaryByCurrency = useMemo(() => {
    const summary = new Map<string, { assets: number; debt: number }>();

    for (const account of accounts) {
      const current = summary.get(account.currency) ?? {
        assets: 0,
        debt: 0,
      };
      const balance = balancesByAccountId.get(account.id)?.balance ?? 0;

      if (account.type === "CREDIT_CARD") {
        current.debt += Math.max(balance, 0);
        current.assets += Math.max(-balance, 0);
      } else {
        current.assets += balance;
      }

      summary.set(account.currency, current);
    }

    return [...summary.entries()].map(([currency, totals]) => ({
      currency,
      assets: totals.assets,
      debt: totals.debt,
      net: totals.assets - totals.debt,
    }));
  }, [accounts, balancesByAccountId]);

  function summaryValue(value: number, currency: string): string {
    return money(value, currency);
  }

  if (accounts.length === 0) {
    return (
      <div className="account-list">
        <section className="accounts-list-heading account-page-header">
          <div>
            <h2>Accounts</h2>
            <p className="accounts-subtitle">0 accounts across your finances</p>
          </div>
          <button
            type="button"
            className="primary-button add-account-trigger"
            onClick={onAddAccount}
          >
            <Plus size={17} aria-hidden="true" />
            Add account
          </button>
        </section>

        <div className="account-list-empty">
          <h3>No accounts yet</h3>
          <p>Add a bank account or cash account to get started.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="account-list">
      <div className="accounts-list-heading account-page-header">
        <div>
          <h2>Accounts</h2>
          <p className="accounts-subtitle">
            {accounts.length} {accounts.length === 1 ? "account" : "accounts"}{" "}
            across your finances
          </p>
        </div>

        <button
          type="button"
          className="primary-button add-account-trigger"
          onClick={onAddAccount}
        >
          <Plus size={17} aria-hidden="true" />
          Add account
        </button>
      </div>

      <div className="account-summary-strip">
        {summaryByCurrency.map(({ currency, assets, debt, net }) => (
          <div className="account-summary-group" key={currency}>
            <p className="account-summary-currency">{currency}</p>

            <div className="account-summary-item">
              <span>Total assets</span>
              <strong>{summaryValue(assets, currency)}</strong>
            </div>

            <div className="account-summary-item">
              <span>Total debt</span>
              <strong className={debt > 0 ? "account-debt-value" : ""}>
                {summaryValue(debt, currency)}
              </strong>
            </div>

            <div className="account-summary-item">
              <span>Net</span>
              <strong>{summaryValue(net, currency)}</strong>
            </div>
          </div>
        ))}
      </div>

      <div className="account-list-grid">
        {accounts.map((account) => {
          const balance = balancesByAccountId.get(account.id);

          return (
            <AccountCard
              key={account.id}
              name={account.name}
              type={account.type}
              currency={account.currency}
              currentBalance={balance?.balance ?? 0}
              creditLimit={balance?.creditLimit ?? account.creditLimit ?? null}
              availableCredit={balance?.availableCredit ?? null}
              parentAccountId={account.parentAccountId ?? null}
              bankAccounts={bankAccounts}
              onRemove={() => onRemoveAccount(account.id)}
              onUpdate={(name, type, currency, creditLimit, parentAccountId) =>
                onUpdateAccount(
                  account.id,
                  name,
                  type,
                  currency,
                  creditLimit,
                  parentAccountId,
                )
              }
            />
          );
        })}
      </div>
    </div>
  );
}

export default AccountList;
