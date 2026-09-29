import { useEffect, useState } from "react";
import AccountList from "./components/AccountList";
import AddAccountForm from "./components/AddAccountForm";
import LoginForm from "./components/LoginForm";
import type { Account, AccountType } from "./types/account";
import type { AuthResponse } from "./types/auth";
import {
  createAccount,
  deleteAccount,
  getAccounts,
  updateAccount,
} from "./api/accountApi";

function App() {
  const [authResponse, setAuthResponse] = useState<AuthResponse | null>(null);

  const [accounts, setAccounts] = useState<Account[]>([]);

  useEffect(() => {
    if (authResponse === null) {
      return;
    }

    getAccounts(authResponse.accessToken).then((accountsFromApi) => {
      setAccounts(accountsFromApi);
    });
  }, [authResponse]);

  function handleLogin(response: AuthResponse) {
    setAuthResponse(response);
  }

  async function addAccount(
    name: string,
    type: AccountType,
    currency: string,
    openingBalance: number,
  ) {
    if (authResponse === null) {
      throw new Error("User is not authenticated.");
    }

    const createdAccount = await createAccount(authResponse.accessToken, {
      name,
      type,
      currency,
      openingBalance,
    });

    // Add the backend-created account to the UI immediately.
    // The backend remains the source of truth.
    setAccounts((currentAccounts) => [...currentAccounts, createdAccount]);
  }

  async function updateExistingAccount(
    accountId: string,
    name: string,
    type: AccountType,
    currency: string,
  ) {
    if (authResponse === null) {
      throw new Error("User is not authenticated.");
    }

    const updatedAccount = await updateAccount(
      authResponse.accessToken,
      accountId,
      {
        name,
        type,
        currency,
      },
    );

    // Replace the old account with the backend's updated version.
    // This keeps the UI synchronized with the backend response.
    setAccounts((currentAccounts) =>
      currentAccounts.map((account) =>
        account.id === accountId ? updatedAccount : account,
      ),
    );
  }

  async function removeAccount(accountId: string) {
    if (authResponse === null) {
      throw new Error("User is not authenticated.");
    }

    // Delete from the backend first.
    // The UI should only change after the backend confirms deletion.
    await deleteAccount(authResponse.accessToken, accountId);

    // The backend returned successfully, so remove the account
    // from the local React state.
    setAccounts((currentAccounts) =>
      currentAccounts.filter((account) => account.id !== accountId),
    );
  }

  if (authResponse === null) {
    return <LoginForm onLogin={handleLogin} />;
  }

  return (
    <>
      <h1>Finance Tracker</h1>

      <p>Welcome, {authResponse.name}!</p>

      <AddAccountForm onAddAccount={addAccount} />

      <AccountList
        accounts={accounts}
        onRemoveAccount={removeAccount}
        onUpdateAccount={updateExistingAccount}
      />
    </>
  );
}

export default App;
