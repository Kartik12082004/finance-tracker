import { useEffect, useState } from "react";
import "./App.css";
import AccountList from "./components/AccountList";
import AddAccountForm from "./components/AddAccountForm";
import AddTransactionForm from "./components/AddTransactionForm";
import Dashboard from "./components/Dashboard";
import LoginForm from "./components/LoginForm";
import RegisterForm from "./components/RegisterForm";
import TransactionList from "./components/TransactionList";
import Goals from "./components/Goals";
import Budgets from "./components/Budgets";
import RecurringTransactions from "./components/RecurringTransactions";
import Investments from "./components/Investments";
import Categories from "./components/Categories";

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

type Section =
  | "dashboard"
  | "accounts"
  | "transactions"
  | "budgets"
  | "goals"
  | "recurring"
  | "investments"
  | "categories";

const navigationItems: {
  id: Section;
  label: string;
  icon: string;
}[] = [
  {
    id: "dashboard",
    label: "Dashboard",
    icon: "⌂",
  },
  {
    id: "accounts",
    label: "Accounts",
    icon: "▣",
  },
  {
    id: "transactions",
    label: "Transactions",
    icon: "↕",
  },
  {
    id: "categories",
    label: "Categories",
    icon: "≡",
  },
  {
    id: "budgets",
    label: "Budgets",
    icon: "◫",
  },
  {
    id: "goals",
    label: "Goals",
    icon: "◎",
  },
  {
    id: "recurring",
    label: "Recurring",
    icon: "↻",
  },
  {
    id: "investments",
    label: "Investments",
    icon: "↗",
  },
];

function App() {
  const [authResponse, setAuthResponse] = useState<AuthResponse | null>(() =>
    getAuthSession(),
  );

  const [showRegister, setShowRegister] = useState(false);

  const [activeSection, setActiveSection] = useState<Section>("dashboard");

  const [mobileNavOpen, setMobileNavOpen] = useState(false);

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
        clearAuthSession();
        setAuthResponse(null);
        setAccounts([]);
        setAccountBalances([]);
        setCategories([]);
        setTransactions([]);
      }
    }

    void loadFinanceData();
  }, [authResponse]);

  function handleLogin(response: AuthResponse) {
    saveAuthSession(response);
    setAuthResponse(response);
    setActiveSection("dashboard");
  }

  function handleRegister(response: AuthResponse) {
    saveAuthSession(response);
    setAuthResponse(response);
    setActiveSection("dashboard");
  }

  function handleLogout() {
    clearAuthSession();
    setAuthResponse(null);
    setAccounts([]);
    setAccountBalances([]);
    setCategories([]);
    setTransactions([]);
    setActiveSection("dashboard");
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

    setAccounts((currentAccounts) => [...currentAccounts, createdAccount]);

    const updatedBalances = await getAccountBalances();
    setAccountBalances(updatedBalances);

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

    await deleteAccount(accountId);

    setAccounts((currentAccounts) =>
      currentAccounts.filter((account) => account.id !== accountId),
    );

    const updatedBalances = await getAccountBalances();
    setAccountBalances(updatedBalances);

    setDashboardRefreshKey((current) => current + 1);
  }

  async function refreshTransactions() {
    const [transactionsFromApi, accountBalancesFromApi] = await Promise.all([
      getTransactions(),
      getAccountBalances(),
    ]);

    setTransactions(transactionsFromApi);
    setAccountBalances(accountBalancesFromApi);

    setDashboardRefreshKey((current) => current + 1);
  }

  async function refreshGoalRelatedFinanceData() {
    const [accountsFromApi, accountBalancesFromApi, transactionsFromApi] =
      await Promise.all([
        getAccounts(),
        getAccountBalances(),
        getTransactions(),
      ]);

    setAccounts(accountsFromApi);
    setAccountBalances(accountBalancesFromApi);
    setTransactions(transactionsFromApi);
    setDashboardRefreshKey((current) => current + 1);
  }

  async function refreshCategories() {
    const categoriesFromApi = await getCategories();
    setCategories(categoriesFromApi);

    setDashboardRefreshKey((current) => current + 1);
  }

  async function handleVoidTransaction(transactionId: string) {
    if (authResponse === null) {
      throw new Error("User is not authenticated.");
    }

    await voidTransaction(transactionId);
    await refreshTransactions();
  }

  function handleNavigation(section: Section) {
    setActiveSection(section);
    setMobileNavOpen(false);
  }

  function renderPage() {
    switch (activeSection) {
      case "dashboard":
        return (
          <Dashboard refreshKey={dashboardRefreshKey} categories={categories} />
        );

      case "accounts":
        return (
          <div className="page-stack">
            <section className="page-section">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Financial accounts</p>
                  <h2>Your Accounts</h2>
                  <p className="section-description">
                    Manage the accounts connected to your finance tracker.
                  </p>
                </div>
              </div>

              <AddAccountForm onAddAccount={addAccount} />
            </section>

            <section className="page-section">
              <AccountList
                accounts={accounts}
                accountBalances={accountBalances}
                onRemoveAccount={removeAccount}
                onUpdateAccount={updateExistingAccount}
              />
            </section>
          </div>
        );

      case "transactions":
        return (
          <div className="page-stack">
            <section className="page-section">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Money movement</p>
                  <h2>Transactions</h2>
                  <p className="section-description">
                    Record and review your income, expenses, and transfers.
                  </p>
                </div>
              </div>

              <AddTransactionForm
                accounts={accounts}
                categories={categories}
                onTransactionCreated={refreshTransactions}
              />
            </section>

            <section className="page-section">
              <TransactionList
                transactions={transactions}
                accounts={accounts}
                categories={categories}
                onVoidTransaction={handleVoidTransaction}
              />
            </section>
          </div>
        );

      case "categories":
        return (
          <div className="page-section">
            <Categories
              categories={categories}
              onCategoriesChanged={refreshCategories}
            />
          </div>
        );

      case "budgets":
        return (
          <div className="page-section">
            <Budgets refreshKey={dashboardRefreshKey} />
          </div>
        );

      case "goals":
        return (
          <div className="page-section">
            <Goals onFinanceDataChanged={refreshGoalRelatedFinanceData} />
          </div>
        );

      case "recurring":
        return (
          <div className="page-section">
            <RecurringTransactions />
          </div>
        );

      case "investments":
        return (
          <div className="page-section">
            <Investments accounts={accounts} />
          </div>
        );

      default:
        return null;
    }
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

  const activeItem = navigationItems.find((item) => item.id === activeSection);

  return (
    <div className="app-shell">
      {mobileNavOpen && (
        <button
          type="button"
          className="mobile-overlay"
          aria-label="Close navigation"
          onClick={() => setMobileNavOpen(false)}
        />
      )}

      <aside className={`sidebar ${mobileNavOpen ? "sidebar-open" : ""}`}>
        <div className="sidebar-brand">
          <div className="brand-mark">F</div>

          <div>
            <div className="brand-name">Finance Tracker</div>
            <div className="brand-subtitle">Personal finance</div>
          </div>
        </div>

        <nav className="sidebar-nav" aria-label="Main navigation">
          <div className="nav-label">Workspace</div>

          {navigationItems.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`nav-item ${
                activeSection === item.id ? "nav-item-active" : ""
              }`}
              onClick={() => handleNavigation(item.id)}
            >
              <span className="nav-icon" aria-hidden="true">
                {item.icon}
              </span>

              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-card">
            <div className="user-avatar">
              {authResponse.name?.charAt(0).toUpperCase() ?? "U"}
            </div>

            <div className="user-details">
              <strong>{authResponse.name}</strong>
              <span>Personal account</span>
            </div>
          </div>

          <button
            type="button"
            className="logout-button"
            onClick={handleLogout}
          >
            <span aria-hidden="true">↪</span>
            Logout
          </button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <button
            type="button"
            className="mobile-menu-button"
            aria-label="Open navigation"
            onClick={() => setMobileNavOpen(true)}
          >
            ☰
          </button>

          <div className="topbar-page-title">
            <span className="topbar-kicker">Finance Tracker</span>
            <strong>{activeItem?.label}</strong>
          </div>

          <div className="topbar-user">
            <div className="topbar-avatar">
              {authResponse.name?.charAt(0).toUpperCase() ?? "U"}
            </div>

            <span>{authResponse.name}</span>
          </div>
        </header>

        <div className="content-wrapper">
          <div className="page-header">
            <div>
              <p className="page-kicker">Overview</p>
              <h1>{activeItem?.label}</h1>
            </div>

            <div className="welcome-message">
              Welcome back, <strong>{authResponse.name}</strong>
            </div>
          </div>

          {renderPage()}
        </div>
      </main>
    </div>
  );
}

export default App;
