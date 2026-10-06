import { useEffect, useState } from "react";
import AccountList from "./components/AccountList";
import AddAccountForm from "./components/AddAccountForm";
import AddTransactionForm from "./components/AddTransactionForm";
import Dashboard from "./components/Dashboard";
import LoginForm from "./components/LoginForm";
import RegisterForm from "./components/RegisterForm";
import TransactionList from "./components/TransactionList";
import type { Account, AccountType } from "./types/account";
import type { AuthResponse } from "./types/auth";
import type { Category } from "./types/category";
import type { Transaction } from "./types/transaction";
import type { AccountBalance } from "./api/analyticsApi";
import {
  createAccount,
  deleteAccount,
  getAccounts,
  updateAccount,
} from "./api/accountApi";
import { getAccountBalances } from "./api/analyticsApi";
import { getCategories } from "./api/categoryApi";
import { getTransactions, voidTransaction } from "./api/transactionApi";
import {
  clearAuthSession,
  getAuthSession,
  saveAuthSession,
} from "./auth/authStorage";
import Goals from "./components/Goals";
import Budgets from "./components/Budgets";
import RecurringTransactions from "./components/RecurringTransactions";
import Investments from "./components/Investments";

function App() {
  // Restore the previous authentication session when the application starts.
  // This allows the user to remain logged in after refreshing the page.
  const [authResponse, setAuthResponse] = useState<AuthResponse | null>(() =>
    getAuthSession(),
  );

  const [showRegister, setShowRegister] = useState(false);

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountBalances, setAccountBalances] = useState<AccountBalance[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [dashboardRefreshKey, setDashboardRefreshKey] = useState(0);

  useEffect(() => {
    if (authResponse === null) {
      return;
    }

    async function loadFinanceData() {
      try {
        const [
          accountsFromApi,
          accountBalancesFromApi,
          categoriesFromApi,
          transactionsFromApi,
        ] = await Promise.all([
          getAccounts(),
          getAccountBalances(),
          getCategories(),
          getTransactions(),
        ]);

        setAccounts(accountsFromApi);
        setAccountBalances(accountBalancesFromApi);
        setCategories(categoriesFromApi);
        setTransactions(transactionsFromApi);
      } catch {
        // The API client clears the session when token refresh fails.
        // We also clear React state so the UI returns to the login screen.
        clearAuthSession();
        setAuthResponse(null);
        setAccounts([]);
        setAccountBalances([]);
        setCategories([]);
        setTransactions([]);
      }
    }

    loadFinanceData();
  }, [authResponse]);

  function handleLogin(response: AuthResponse) {
    saveAuthSession(response);
    setAuthResponse(response);
  }

  function handleRegister(response: AuthResponse) {
    // Registration already returns authenticated session tokens,
    // so the newly registered user can enter the application immediately.
    saveAuthSession(response);
    setAuthResponse(response);
  }

  function handleLogout() {
    clearAuthSession();
    setAuthResponse(null);
    setAccounts([]);
    setAccountBalances([]);
    setCategories([]);
    setTransactions([]);
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

    // A newly created account starts with its opening balance.
    // Fetching balances keeps the calculated balance synchronized.
    const updatedBalances = await getAccountBalances();
    setAccountBalances(updatedBalances);

    // Refresh dashboard analytics because account balances changed.
    setDashboardRefreshKey((current) => current + 1);
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

    const updatedBalances = await getAccountBalances();
    setAccountBalances(updatedBalances);

    // Refresh dashboard analytics because account balances changed.
    setDashboardRefreshKey((current) => current + 1);
  }

  async function refreshTransactions() {
    const [transactionsFromApi, accountBalancesFromApi] = await Promise.all([
      getTransactions(),
      getAccountBalances(),
    ]);

    setTransactions(transactionsFromApi);
    setAccountBalances(accountBalancesFromApi);

    // Refresh dashboard analytics after a transaction is created or voided.
    setDashboardRefreshKey((current) => current + 1);
  }

  async function handleVoidTransaction(transactionId: string) {
    if (authResponse === null) {
      throw new Error("User is not authenticated.");
    }

    await voidTransaction(transactionId);
    await refreshTransactions();
  }

  if (authResponse === null) {
    if (showRegister) {
      return (
        <RegisterForm
          onRegister={handleRegister}
          onSwitchToLogin={() => setShowRegister(false)}
        />
      );
    }

    return (
      <LoginForm
        onLogin={handleLogin}
        onSwitchToRegister={() => setShowRegister(true)}
      />
    );
  }

  return (
    <>
      <h1>Finance Tracker</h1>

      <p>Welcome, {authResponse.name}!</p>

      <button type="button" onClick={handleLogout}>
        Logout
      </button>

      <Dashboard refreshKey={dashboardRefreshKey} />

      <Goals />

      <Budgets refreshKey={dashboardRefreshKey} />

      <RecurringTransactions />

      <Investments accounts={accounts} />

      <AddAccountForm onAddAccount={addAccount} />

      <AccountList
        accounts={accounts}
        accountBalances={accountBalances}
        onRemoveAccount={removeAccount}
        onUpdateAccount={updateExistingAccount}
      />

      <AddTransactionForm
        accounts={accounts}
        categories={categories}
        onTransactionCreated={refreshTransactions}
      />

      <TransactionList
        transactions={transactions}
        accounts={accounts}
        categories={categories}
        onVoidTransaction={handleVoidTransaction}
      />
    </>
  );
}

export default App;
