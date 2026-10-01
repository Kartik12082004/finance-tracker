import AccountCard from "./AccountCard";
import type { Account, AccountType } from "../types/account";
import type { AccountBalance } from "../api/analyticsApi";

type AccountListProps = {
  accounts: Account[];
  accountBalances: AccountBalance[];
  onRemoveAccount: (id: string) => Promise<void>;
  onUpdateAccount: (
    id: string,
    name: string,
    type: AccountType,
    currency: string,
  ) => Promise<void>;
};

function AccountList({
  accounts,
  accountBalances,
  onRemoveAccount,
  onUpdateAccount,
}: AccountListProps) {
  if (accounts.length === 0) {
    return <p>No accounts yet.</p>;
  }

  function getCurrentBalance(accountId: string): number {
    return (
      accountBalances.find((balance) => balance.accountId === accountId)
        ?.balance ?? 0
    );
  }

  return (
    <div>
      <p>You have {accounts.length} accounts.</p>

      {accounts.map((account) => (
        <AccountCard
          key={account.id}
          name={account.name}
          type={account.type}
          currency={account.currency}
          openingBalance={account.openingBalance}
          currentBalance={getCurrentBalance(account.id)}
          onRemove={() => onRemoveAccount(account.id)}
          onUpdate={(name, type, currency) =>
            onUpdateAccount(account.id, name, type, currency)
          }
        />
      ))}
    </div>
  );
}

export default AccountList;
