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
import {
  clearAuthSession,
  getAuthSession,
  saveAuthSession,
} from "./auth/authStorage";

function App() {
  // Restore the previous authentication session when the application starts.
  // This allows the user to remain logged in after refreshing the page.
  const [authResponse, setAuthResponse] = useState<AuthResponse | null>(() =>
    getAuthSession(),
  );

  const [accounts, setAccounts] = useState<Account[]>([]);

  useEffect(() => {
    if (authResponse === null) {
      return;
    }

    getAccounts()
      .then((accountsFromApi) => {
        setAccounts(accountsFromApi);
      })
      .catch(() => {
        // The API client clears the session when token refresh fails.
        // We also clear React state so the UI returns to the login screen.
        clearAuthSession();
        setAuthResponse(null);
        setAccounts([]);
      });
  }, [authResponse]);

  function handleLogin(response: AuthResponse) {
    // Keep React state and browser session storage synchronized.
    saveAuthSession(response);
    setAuthResponse(response);
  }

  function handleLogout() {
    clearAuthSession();
    setAuthResponse(null);
    setAccounts([]);
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

    const createdAccount = await createAccount({
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

    const updatedAccount = await updateAccount(accountId, {
      name,
      type,
      currency,
    });

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
    await deleteAccount(accountId);

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

      <button type="button" onClick={handleLogout}>
        Logout
      </button>

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
