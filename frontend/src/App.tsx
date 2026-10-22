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

import AuthLayout from "./components/AuthLayout";

import type { LucideIcon } from "lucide-react";

import {
  ArrowLeftRight,
  CalendarClock,
  ChartNoAxesCombined,
  ChevronDown,
  ChevronUp,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  Tags,
  Target,
  Wallet,
} from "lucide-react";

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

const navigationGroups: {
  label: string;
  items: {
    id: Exclude<Section, "settings">;
    label: string;
    icon: LucideIcon;
  }[];
}[] = [
  {
    label: "Overview",
    items: [{ id: "dashboard", label: "Dashboard", icon: LayoutDashboard }],
  },
  {
    label: "Money",
    items: [
      { id: "transactions", label: "Transactions", icon: ArrowLeftRight },
      { id: "accounts", label: "Accounts", icon: Wallet },
      { id: "categories", label: "Categories", icon: Tags },
    ],
  },
  {
    label: "Planning",
    items: [
      { id: "budgets", label: "Budgets", icon: ChartNoAxesCombined },
      { id: "goals", label: "Goals", icon: Target },
      { id: "recurring", label: "Recurring", icon: CalendarClock },
    ],
  },
  {
    label: "Wealth",
    items: [
      { id: "investments", label: "Investments", icon: ChartNoAxesCombined },
    ],
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

    if (isAccountLinkError) return "";

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
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountBalances, setAccountBalances] = useState<AccountBalance[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [dashboardRefreshKey, setDashboardRefreshKey] = useState(0);
  const [isLoadingFinanceData, setIsLoadingFinanceData] = useState(false);
  const [financeDataError, setFinanceDataError] = useState("");
  const [financeDataRefreshKey, setFinanceDataRefreshKey] = useState(0);

  const [duplicateTransaction, setDuplicateTransaction] =
    useState<Transaction | null>(null);

  const [transactionModalOpenSignal, setTransactionModalOpenSignal] =
    useState(0);

  // AccountList owns the Add Account button. This callback opens the form modal.
  const [openAddAccountModal, setOpenAddAccountModal] = useState<
    (() => void) | null
  >(null);

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
      if (!event.persisted) return;

      setIsLinkingAccount(false);
      setUnlinkingProvider(null);
      setIsLoadingAccountLinkStatus(false);
      setAccountLinkRefreshKey((current) => current + 1);
    }

    window.addEventListener("pageshow", handlePageShow);

    return () => {
      window.removeEventListener("pageshow", handlePageShow);
    };
  }, []);

  useEffect(() => {
    if (oauthCallbackHandled.current) return;

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

    if (linkedProvider === "github" || linkedProvider === "google") {
      navigate("/settings", { replace: true });
      return;
    }

    if (
      providerError === "github_link_failed" ||
      providerError === "google_link_failed" ||
      providerError === "account_link_failed"
    ) {
      navigate("/settings", { replace: true });
      return;
    }

    if (!code) return;

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
    if (authResponse === null) return;

    let cancelled = false;

    async function loadFinanceData() {
      setIsLoadingFinanceData(true);
      setFinanceDataError("");

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

        if (cancelled) return;

        setAccounts(accountsFromApi);
        setAccountBalances(accountBalancesFromApi);
        setCategories(categoriesFromApi);
        setTransactions(transactionsFromApi);
      } catch (error) {
        if (cancelled) return;

        const message =
          error instanceof Error
            ? error.message
            : "Could not load your finance data. Please try again.";

        if (message === "Authentication session expired.") {
          clearAuthSession();
          setAuthResponse(null);
          setAccounts([]);
          setAccountBalances([]);
          setCategories([]);
          setTransactions([]);
          return;
        }

        setFinanceDataError(
          "Could not load your finance data. Check your connection and try again.",
        );
      } finally {
        if (!cancelled) setIsLoadingFinanceData(false);
      }
    }

    void loadFinanceData();

    return () => {
      cancelled = true;
    };
  }, [authResponse, financeDataRefreshKey]);

  useEffect(() => {
    if (authResponse === null || activeSection !== "settings") return;

    let cancelled = false;

    async function loadAccountLinkStatus() {
      setIsLoadingAccountLinkStatus(true);

      try {
        const status = await getAccountLinkStatus();

        if (!cancelled) setAccountLinkStatus(status);
      } catch {
        if (!cancelled) {
          setAccountLinkMessage(
            "Could not load connected-account status. Please try again.",
          );
        }
      } finally {
        if (!cancelled) setIsLoadingAccountLinkStatus(false);
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
    creditLimit: number | null,
    parentAccountId: string | null,
  ) {
    if (authResponse === null) {
      throw new Error("User is not authenticated.");
    }

    const createdAccount = await createAccount({
      name,
      type,
      currency,
      openingBalance,
      creditLimit,
      parentAccountId,
    });

    setAccounts((current) => [...current, createdAccount]);

    const updatedBalances = await getAccountBalances();
    setAccountBalances(updatedBalances);
    setDashboardRefreshKey((current) => current + 1);
  }

  async function updateExistingAccount(
    accountId: string,
    name: string,
    type: AccountType,
    currency: string,
    creditLimit: number | null,
    parentAccountId: string | null,
  ) {
    if (authResponse === null) {
      throw new Error("User is not authenticated.");
    }

    const updatedAccount = await updateAccount(accountId, {
      name,
      type,
      currency,
      creditLimit,
      parentAccountId,
    });

    setAccounts((current) =>
      current.map((account) =>
        account.id === accountId ? updatedAccount : account,
      ),
    );

    const updatedBalances = await getAccountBalances();
    setAccountBalances(updatedBalances);
    setDashboardRefreshKey((current) => current + 1);
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
    setAccountMenuOpen(false);
  }

  function renderPage() {
    switch (activeSection) {
      case "dashboard":
        return (
          <Dashboard refreshKey={dashboardRefreshKey} categories={categories} />
        );

      case "accounts":
        return (
          <div className="page-stack accounts-page">
            <section className="page-section accounts-section">
              <AddAccountForm
                accounts={accounts}
                onAddAccount={addAccount}
                onOpen={(open) => setOpenAddAccountModal(() => open)}
              />

              <AccountList
                accounts={accounts}
                accountBalances={accountBalances}
                onAddAccount={() => openAddAccountModal?.()}
                onRemoveAccount={removeAccount}
                onUpdateAccount={updateExistingAccount}
              />
            </section>
          </div>
        );

      case "transactions":
        return (
          <div className="page-stack transactions-page">
            <section className="page-section transactions-section">
              <AddTransactionForm
                accounts={accounts}
                categories={categories}
                accountBalances={accountBalances}
                onTransactionCreated={refreshTransactions}
                duplicateTransaction={duplicateTransaction}
                onDuplicateConsumed={() => setDuplicateTransaction(null)}
                openSignal={transactionModalOpenSignal}
                hideTrigger
              />

              <TransactionList
                transactions={transactions}
                accounts={accounts}
                categories={categories}
                onVoidTransaction={handleVoidTransaction}
                onDuplicateTransaction={setDuplicateTransaction}
                onAddTransaction={() =>
                  setTransactionModalOpenSignal((current) => current + 1)
                }
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
          <div className="page-stack settings-page">
            <header className="settings-page-header">
              <div>
                <p className="page-kicker">Preferences & security</p>
                <h1>Account Settings</h1>
                <p className="settings-page-description">
                  Manage how you sign in and which accounts are connected to
                  your Finance Tracker profile.
                </p>
              </div>
              <div className="settings-header-icon" aria-hidden="true">
                <Settings size={24} />
              </div>
            </header>

            <section
              className="page-section settings-section"
              aria-labelledby="connected-accounts-heading"
            >
              <div className="settings-section-heading">
                <div className="settings-section-icon" aria-hidden="true">
                  <Wallet size={19} />
                </div>
                <div>
                  <h2 id="connected-accounts-heading">Connected accounts</h2>
                  <p className="section-description">
                    Connect Google or GitHub for convenient sign-in, or
                    disconnect an account you no longer use.
                  </p>
                </div>
              </div>

              {isLoadingAccountLinkStatus && (
                <div className="settings-feedback" role="status">
                  <span className="settings-loading-indicator" />
                  <span>Loading your connected accounts...</span>
                </div>
              )}

              {!isLoadingAccountLinkStatus && accountLinkStatus === null && (
                <div className="settings-load-error" role="alert">
                  <div>
                    <strong>Couldn&apos;t load connected accounts</strong>
                    <p>
                      Check your connection and try loading the status again.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="settings-button settings-button-primary"
                    onClick={() => {
                      setAccountLinkStatus(null);
                      setAccountLinkRefreshKey((current) => current + 1);
                    }}
                    disabled={isLoadingAccountLinkStatus}
                  >
                    Retry
                  </button>
                </div>
              )}

              {accountLinkStatus !== null && (
                <div className="settings-provider-list">
                  <article className="settings-provider-card">
                    <div className="settings-provider-identity">
                      <div className="settings-provider-logo settings-google-logo">
                        <svg viewBox="0 0 48 48" aria-hidden="true">
                          <path
                            fill="#4285F4"
                            d="M43.6 24.5c0-1.6-.1-3.1-.4-4.5H24v8.5h11a9.4 9.4 0 0 1-4.1 6.2v5.5h6.7c3.9-3.6 6-9 6-15.7Z"
                          />
                          <path
                            fill="#34A853"
                            d="M24 44c5.5 0 10.1-1.8 13.5-4.8l-6.7-5.5c-1.8 1.2-4 2-6.8 2-5.2 0-9.6-3.5-11.2-8.2H5.9v5.7A20 20 0 0 0 24 44Z"
                          />
                          <path
                            fill="#FBBC05"
                            d="M12.8 27.5a12 12 0 0 1 0-7v-5.7H5.9a20 20 0 0 0 0 18.4l6.9-5.7Z"
                          />
                          <path
                            fill="#EA4335"
                            d="M24 12.3c3 0 5.7 1 7.8 3.1l5.9-5.9A19.7 19.7 0 0 0 24 4 20 20 0 0 0 5.9 14.8l6.9 5.7c1.6-4.7 6-8.2 11.2-8.2Z"
                          />
                        </svg>
                      </div>
                      <div className="settings-provider-details">
                        <h3>Google</h3>
                        <p>
                          {accountLinkStatus.googleLinked
                            ? "Your Google account is connected."
                            : "Sign in using your Google account."}
                        </p>
                      </div>
                    </div>

                    <div className="settings-provider-actions">
                      <span
                        className={`settings-status ${
                          accountLinkStatus.googleLinked
                            ? "settings-status-connected"
                            : "settings-status-disconnected"
                        }`}
                      >
                        <span className="settings-status-dot" />
                        {accountLinkStatus.googleLinked
                          ? "Connected"
                          : "Not connected"}
                      </span>

                      {accountLinkStatus.googleLinked ? (
                        <button
                          type="button"
                          className="settings-button settings-button-danger"
                          onClick={() => void handleUnlinkAccount("google")}
                          disabled={
                            isLinkingAccount || unlinkingProvider !== null
                          }
                        >
                          {unlinkingProvider === "google"
                            ? "Disconnecting..."
                            : "Disconnect"}
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="settings-button settings-button-primary"
                          onClick={() => void handleLinkAccount("google")}
                          disabled={
                            isLinkingAccount || unlinkingProvider !== null
                          }
                        >
                          {isLinkingAccount ? "Connecting..." : "Connect"}
                        </button>
                      )}
                    </div>
                  </article>

                  <article className="settings-provider-card">
                    <div className="settings-provider-identity">
                      <div className="settings-provider-logo settings-github-logo">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path
                            fill="currentColor"
                            d="M12 .9a11.1 11.1 0 0 0-3.5 21.6c.6.1.8-.3.8-.6v-2.1c-3.1.7-3.8-1.3-3.8-1.3-.5-1.3-1.3-1.6-1.3-1.6-1-.7.1-.7.1-.7 1.1.1 1.7 1.2 1.7 1.2 1 1.7 2.6 1.2 3.3.9.1-.7.4-1.2.7-1.5-2.5-.3-5.1-1.3-5.1-5.5 0-1.2.4-2.2 1.2-3-.1-.3-.5-1.4.1-2.9 0 0 1-.3 3.1 1.2a10.7 10.7 0 0 1 5.6 0C17 5.2 18 5.5 18 5.5c.6 1.5.2 2.6.1 2.9.8.8 1.2 1.8 1.2 3 0 4.2-2.6 5.2-5.1 5.5.4.3.7 1 .7 1.9v3.1c0 .3.2.7.8.6A11.1 11.1 0 0 0 12 .9Z"
                          />
                        </svg>
                      </div>
                      <div className="settings-provider-details">
                        <h3>GitHub</h3>
                        <p>
                          {accountLinkStatus.githubLinked
                            ? "Your GitHub account is connected."
                            : "Sign in using your GitHub account."}
                        </p>
                      </div>
                    </div>

                    <div className="settings-provider-actions">
                      <span
                        className={`settings-status ${
                          accountLinkStatus.githubLinked
                            ? "settings-status-connected"
                            : "settings-status-disconnected"
                        }`}
                      >
                        <span className="settings-status-dot" />
                        {accountLinkStatus.githubLinked
                          ? "Connected"
                          : "Not connected"}
                      </span>

                      {accountLinkStatus.githubLinked ? (
                        <button
                          type="button"
                          className="settings-button settings-button-danger"
                          onClick={() => void handleUnlinkAccount("github")}
                          disabled={
                            isLinkingAccount || unlinkingProvider !== null
                          }
                        >
                          {unlinkingProvider === "github"
                            ? "Disconnecting..."
                            : "Disconnect"}
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="settings-button settings-button-primary"
                          onClick={() => void handleLinkAccount("github")}
                          disabled={
                            isLinkingAccount || unlinkingProvider !== null
                          }
                        >
                          {isLinkingAccount ? "Connecting..." : "Connect"}
                        </button>
                      )}
                    </div>
                  </article>
                </div>
              )}

              <div className="settings-security-note">
                <div className="settings-security-icon" aria-hidden="true">
                  <Settings size={17} />
                </div>
                <p>
                  <strong>Keep your sign-in options accessible.</strong> Before
                  disconnecting a provider, make sure you can still access your
                  account using another available sign-in method.
                </p>
              </div>
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
        <AuthLayout variant="register">
          <RegisterForm
            onRegister={handleRegister}
            onSwitchToLogin={() => {
              navigate("/login");
            }}
          />
        </AuthLayout>
      );
    }

    return (
      <AuthLayout variant="login">
        {oauthError && <p role="alert">{oauthError}</p>}

        <LoginForm
          onLogin={handleLogin}
          onSwitchToRegister={() => {
            navigate("/register");
          }}
        />
      </AuthLayout>
    );
  }

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
          {navigationGroups.map((group) => (
            <div className="nav-group" key={group.label}>
              <div className="nav-label">{group.label}</div>
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive = activeSection === item.id;

                return (
                  <button
                    key={item.id}
                    type="button"
                    className={`nav-item ${isActive ? "nav-item-active" : ""}`}
                    aria-current={isActive ? "page" : undefined}
                    onClick={() => handleNavigation(item.id)}
                  >
                    <Icon
                      className="nav-icon"
                      size={18}
                      strokeWidth={isActive ? 2.3 : 1.9}
                      aria-hidden="true"
                    />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button
            type="button"
            className={`user-card account-menu-trigger ${
              accountMenuOpen ? "account-menu-trigger-open" : ""
            }`}
            aria-haspopup="true"
            aria-expanded={accountMenuOpen}
            onClick={() => setAccountMenuOpen((open) => !open)}
          >
            <div className="user-avatar">
              {authResponse.name?.charAt(0).toUpperCase() ?? "U"}
            </div>

            <div className="user-details">
              <strong>{authResponse.name}</strong>
              <span>Personal account</span>
            </div>

            <span className="account-menu-chevron" aria-hidden="true">
              {accountMenuOpen ? (
                <ChevronDown size={16} />
              ) : (
                <ChevronUp size={16} />
              )}
            </span>
          </button>

          {accountMenuOpen && (
            <div className="account-menu sidebar-account-menu">
              <button
                type="button"
                className="account-menu-item"
                onClick={() => handleNavigation("settings")}
              >
                <Settings size={16} aria-hidden="true" />
                Account Settings
              </button>

              <button
                type="button"
                className="account-menu-item account-menu-logout"
                onClick={handleLogout}
              >
                <LogOut size={16} aria-hidden="true" />
                Log out
              </button>
            </div>
          )}
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar" aria-label="Mobile header">
          <button
            type="button"
            className="mobile-menu-button"
            aria-label="Open navigation"
            aria-expanded={mobileNavOpen}
            onClick={() => setMobileNavOpen(true)}
          >
            <Menu size={20} aria-hidden="true" />
          </button>
          <div className="mobile-header-brand">
            <span className="mobile-header-mark">F</span>
            <span>Finance Tracker</span>
          </div>
          <button
            type="button"
            className="mobile-account-button"
            aria-label="Open account menu"
            aria-expanded={accountMenuOpen}
            onClick={() => {
              setMobileNavOpen(true);
              setAccountMenuOpen(true);
            }}
          >
            <span className="mobile-account-avatar">
              {authResponse.name?.charAt(0).toUpperCase() ?? "U"}
            </span>
          </button>
        </header>

        <div className="content-wrapper">
          {isLoadingFinanceData && (
            <p role="status">Loading your finance data...</p>
          )}

          {financeDataError && (
            <div role="alert">
              <p>{financeDataError}</p>
              <button
                type="button"
                onClick={() =>
                  setFinanceDataRefreshKey((current) => current + 1)
                }
                disabled={isLoadingFinanceData}
              >
                {isLoadingFinanceData ? "Retrying..." : "Retry loading data"}
              </button>
            </div>
          )}

          {accountLinkMessage && (
            <p role="status" aria-live="polite">
              {accountLinkMessage}
            </p>
          )}

          {activeSection === "dashboard" ? (
            <div className="page-header">
              <div>
                <p className="page-kicker">Overview</p>
                <h1>Dashboard</h1>
              </div>

              <div className="welcome-message">
                Welcome back, <strong>{authResponse.name}</strong>
              </div>
            </div>
          ) : null}

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
