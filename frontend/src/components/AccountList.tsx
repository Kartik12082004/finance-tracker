import AccountCard from "./AccountCard";
import type { Account, AccountType } from "../types/account";

type AccountListProps = {
  accounts: Account[];
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
  onRemoveAccount,
  onUpdateAccount,
}: AccountListProps) {
  if (accounts.length === 0) {
    return <p>No accounts yet.</p>;
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
