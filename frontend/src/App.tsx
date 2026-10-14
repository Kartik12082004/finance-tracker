import { useEffect, useRef, useState } from "react";
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

import { exchangeOAuthCode } from "./api/authApi";

import {
  getAccountLinkStatus,
  initiateGitHubAccountLink,
  initiateGoogleAccountLink,
  unlinkGitHubAccount,
  unlinkGoogleAccount,
} from "./api/accountLinkApi";

import type { AccountLinkStatus } from "./api/accountLinkApi";

import {
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";

type Section =
  | "dashboard"
  | "accounts"
  | "transactions"
  | "budgets"
  | "goals"
  | "recurring"
  | "investments"
  | "categories"
  | "settings";

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
  {
    id: "settings",
    label: "Account Settings",
    icon: "⚙",
  },
];

function App() {
  const [authResponse, setAuthResponse] = useState<AuthResponse | null>(() =>
    getAuthSession(),
  );

  const oauthCallbackHandled = useRef(false);

  const [isOAuthProcessing, setIsOAuthProcessing] = useState(() =>
    window.location.hash.includes("code="),
  );

  const [oauthError, setOAuthError] = useState(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const providerError = params.get("error");

    const isAccountLinkError =
      providerError === "github_link_failed" ||
      providerError === "google_link_failed" ||
      providerError === "account_link_failed";

    if (isAccountLinkError) {
      return "";
    }

    if (providerError === "oauth_email_exists") {
      return "An account with this email already exists. Log in with that account first.";
    }

    return !params.get("code") && providerError
      ? "OAuth sign-in was cancelled or failed."
      : "";
  });

  const [accountLinkMessage, setAccountLinkMessage] = useState(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const linkedProvider = params.get("linked");
    const error = params.get("error");

    if (linkedProvider === "github") {
      return "GitHub account linked successfully.";
    }

    if (linkedProvider === "google") {
      return "Google account linked successfully.";
    }

    if (error === "github_link_failed") {
      return "GitHub linking failed. The account may already be linked elsewhere, or authorization was unsuccessful.";
    }

    if (error === "google_link_failed") {
      return "Google linking failed. The account may already be linked elsewhere, or authorization was unsuccessful.";
    }

    if (error === "account_link_failed") {
      return "Account linking failed. Please try again.";
    }

    return "";
  });

  const location = useLocation();
  const navigate = useNavigate();

  const routeToSection: Record<string, Section> = {
    "/dashboard": "dashboard",
    "/accounts": "accounts",
    "/transactions": "transactions",
    "/categories": "categories",
    "/budgets": "budgets",
    "/goals": "goals",
    "/recurring": "recurring",
    "/investments": "investments",
    "/settings": "settings",
  };

  const activeSection: Section =
    routeToSection[location.pathname] ??
    (location.pathname === "/oauth/callback" && authResponse !== null
      ? "settings"
      : "dashboard");

  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountBalances, setAccountBalances] = useState<AccountBalance[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [dashboardRefreshKey, setDashboardRefreshKey] = useState(0);

  const [accountLinkStatus, setAccountLinkStatus] =
    useState<AccountLinkStatus | null>(null);

  const [isLoadingAccountLinkStatus, setIsLoadingAccountLinkStatus] =
    useState(false);

  const [accountLinkRefreshKey, setAccountLinkRefreshKey] = useState(0);
  const [isLinkingAccount, setIsLinkingAccount] = useState(false);

  const [unlinkingProvider, setUnlinkingProvider] = useState<
    "google" | "github" | null
  >(null);

  useEffect(() => {
    function handlePageShow(event: PageTransitionEvent) {
      // Only reset state when the browser restores a page
      // from its back-forward cache.
      if (!event.persisted) {
        return;
      }

      setIsLinkingAccount(false);
      setUnlinkingProvider(null);
      setIsLoadingAccountLinkStatus(false);

      // Refresh connected-account status if Settings is open.
      setAccountLinkRefreshKey((current) => current + 1);
    }

    window.addEventListener("pageshow", handlePageShow);

    return () => {
      window.removeEventListener("pageshow", handlePageShow);
    };
  }, []);

  useEffect(() => {
    if (oauthCallbackHandled.current) {
      return;
    }

    const hash = window.location.hash;

    if (!hash) {
      if (
        authResponse !== null &&
        location.pathname === "/oauth/callback" &&
        !isOAuthProcessing
      ) {
        navigate("/settings", { replace: true });
      }

      return;
    }

    const params = new URLSearchParams(hash.slice(1));
    const code = params.get("code");
    const providerError = params.get("error");
    const linkedProvider = params.get("linked");

    if (!code && !providerError && !linkedProvider) return;

    oauthCallbackHandled.current = true;

    window.history.replaceState(
      {},
      document.title,
      window.location.pathname + window.location.search,
    );

    // Account-link success messages are initialized from the URL hash.
    if (linkedProvider === "github" || linkedProvider === "google") {
      navigate("/settings", { replace: true });
      return;
    }

    // Account-link errors are displayed on Account Settings.
    if (
      providerError === "github_link_failed" ||
      providerError === "google_link_failed" ||
      providerError === "account_link_failed"
    ) {
      navigate("/settings", { replace: true });
      return;
    }

    // OAuth sign-in errors are initialized from the URL hash.
    if (!code) {
      return;
    }

    void exchangeOAuthCode(code)
      .then((response) => {
        saveAuthSession(response);
        setAuthResponse(response);
        navigate("/dashboard", { replace: true });
      })
      .catch(() => {
        setOAuthError("Google/GitHub sign-in failed. Please try again.");
      })
      .finally(() => setIsOAuthProcessing(false));
  }, [navigate, authResponse, location.pathname, isOAuthProcessing]);

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

  useEffect(() => {
    if (authResponse === null || activeSection !== "settings") {
      return;
    }

    let cancelled = false;

    async function loadAccountLinkStatus() {
      setIsLoadingAccountLinkStatus(true);

      try {
        const status = await getAccountLinkStatus();

        if (!cancelled) {
          setAccountLinkStatus(status);
        }
      } catch {
        if (!cancelled) {
          setAccountLinkMessage(
            "Could not load connected-account status. Please try again.",
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoadingAccountLinkStatus(false);
        }
      }
    }

    void loadAccountLinkStatus();

    return () => {
      cancelled = true;
    };
  }, [authResponse, activeSection, accountLinkRefreshKey]);

  function handleLogin(response: AuthResponse) {
    saveAuthSession(response);
    setAuthResponse(response);
    navigate("/dashboard", { replace: true });
  }

  function handleRegister(response: AuthResponse) {
    saveAuthSession(response);
    setAuthResponse(response);
    navigate("/dashboard", { replace: true });
  }

  async function handleLinkAccount(provider: "google" | "github") {
    setAccountLinkMessage("");
    setIsLinkingAccount(true);

    try {
      if (provider === "google") {
        await initiateGoogleAccountLink();
      } else {
        await initiateGitHubAccountLink();
      }

      window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
    } catch (error) {
      setAccountLinkMessage(
        error instanceof Error
          ? error.message
          : `Could not start ${provider} account linking.`,
      );

      setIsLinkingAccount(false);
    }
  }

  async function handleUnlinkAccount(provider: "google" | "github") {
    const providerName = provider === "google" ? "Google" : "GitHub";

    const confirmed = window.confirm(
      `Disconnect your ${providerName} account? You may lose this sign-in option.`,
    );

    if (!confirmed) return;

    setAccountLinkMessage("");
    setUnlinkingProvider(provider);

    try {
      if (provider === "google") {
        await unlinkGoogleAccount();
      } else {
        await unlinkGitHubAccount();
      }

      setAccountLinkMessage(
        `${providerName} account disconnected successfully.`,
      );

      setAccountLinkRefreshKey((current) => current + 1);
    } catch (error) {
      setAccountLinkMessage(
        error instanceof Error && error.message
          ? error.message
          : `Could not disconnect ${providerName}. You may need to keep at least one sign-in method connected.`,
      );
    } finally {
      setUnlinkingProvider(null);
    }
  }

  function handleLogout() {
    clearAuthSession();
    setAuthResponse(null);
    setAccounts([]);
    setAccountBalances([]);
    setCategories([]);
    setTransactions([]);
    setAccountLinkStatus(null);
    setAccountLinkMessage("");
    navigate("/login", { replace: true });
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
    navigate(`/${section}`);
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

      case "settings":
        return (
          <div className="page-section">
            <h2>Account Settings</h2>
            <p>Manage your sign-in methods and connected accounts.</p>

            <section aria-labelledby="connected-accounts-heading">
              <h3 id="connected-accounts-heading">Connected accounts</h3>

              {isLoadingAccountLinkStatus && (
                <p role="status">Loading connected accounts...</p>
              )}

              {!isLoadingAccountLinkStatus && accountLinkStatus === null && (
                <button
                  type="button"
                  onClick={() => {
                    setAccountLinkStatus(null);
                    setAccountLinkRefreshKey((current) => current + 1);
                  }}
                >
                  Retry loading connected accounts
                </button>
              )}

              {accountLinkStatus !== null && (
                <div>
                  <div>
                    <h4>Google</h4>

                    <p>
                      {accountLinkStatus.googleLinked
                        ? "Connected"
                        : "Not connected"}
                    </p>

                    {accountLinkStatus.googleLinked ? (
                      <button
                        type="button"
                        onClick={() => void handleUnlinkAccount("google")}
                        disabled={
                          isLinkingAccount || unlinkingProvider !== null
                        }
                      >
                        {unlinkingProvider === "google"
                          ? "Disconnecting..."
                          : "Disconnect Google"}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => void handleLinkAccount("google")}
                        disabled={
                          isLinkingAccount || unlinkingProvider !== null
                        }
                      >
                        {isLinkingAccount ? "Connecting..." : "Connect Google"}
                      </button>
                    )}
                  </div>

                  <div>
                    <h4>GitHub</h4>

                    <p>
                      {accountLinkStatus.githubLinked
                        ? "Connected"
                        : "Not connected"}
                    </p>

                    {accountLinkStatus.githubLinked ? (
                      <button
                        type="button"
                        onClick={() => void handleUnlinkAccount("github")}
                        disabled={
                          isLinkingAccount || unlinkingProvider !== null
                        }
                      >
                        {unlinkingProvider === "github"
                          ? "Disconnecting..."
                          : "Disconnect GitHub"}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => void handleLinkAccount("github")}
                        disabled={
                          isLinkingAccount || unlinkingProvider !== null
                        }
                      >
                        {isLinkingAccount ? "Connecting..." : "Connect GitHub"}
                      </button>
                    )}
                  </div>
                </div>
              )}
            </section>
          </div>
        );

      default:
        return null;
    }
  }

  // Keep unauthenticated users away from protected pages.
  if (
    authResponse === null &&
    location.pathname !== "/login" &&
    location.pathname !== "/register" &&
    !(location.pathname === "/oauth/callback" && isOAuthProcessing)
  ) {
    return <Navigate to="/login" replace />;
  }

  // Keep authenticated users out of the login and registration pages.
  if (
    authResponse !== null &&
    (location.pathname === "/login" || location.pathname === "/register")
  ) {
    return <Navigate to="/dashboard" replace />;
  }

  if (authResponse === null) {
    if (isOAuthProcessing) {
      return <p>Completing sign-in...</p>;
    }

    const isRegisterPage = location.pathname === "/register";

    if (isRegisterPage) {
      return (
        <RegisterForm
          onRegister={handleRegister}
          onSwitchToLogin={() => {
            navigate("/login");
          }}
        />
      );
    }

    return (
      <>
        {oauthError && <p role="alert">{oauthError}</p>}

        <LoginForm
          onLogin={handleLogin}
          onSwitchToRegister={() => {
            navigate("/register");
          }}
        />
      </>
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
          {accountLinkMessage && (
            <p role="status" aria-live="polite">
              {accountLinkMessage}
            </p>
          )}

          <div className="page-header">
            <div>
              <p className="page-kicker">Overview</p>
              <h1>{activeItem?.label}</h1>
            </div>

            <div className="welcome-message">
              Welcome back, <strong>{authResponse.name}</strong>
            </div>
          </div>

          <Routes>
            <Route path="/dashboard" element={renderPage()} />
            <Route path="/accounts" element={renderPage()} />
            <Route path="/transactions" element={renderPage()} />
            <Route path="/categories" element={renderPage()} />
            <Route path="/budgets" element={renderPage()} />
            <Route path="/goals" element={renderPage()} />
            <Route path="/recurring" element={renderPage()} />
            <Route path="/investments" element={renderPage()} />
            <Route path="/settings" element={renderPage()} />
            <Route path="/oauth/callback" element={renderPage()} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </div>
      </main>
    </div>
  );
}

export default App;
