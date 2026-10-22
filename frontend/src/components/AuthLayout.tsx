import type { ReactNode } from "react";

type AuthLayoutProps = {
  children: ReactNode;
  variant: "login" | "register";
};

const registrationBenefits = [
  "Track income and expenses",
  "Set budgets and financial goals",
  "Monitor investments",
  "Keep everything organized",
];

function AuthLayout({ children, variant }: AuthLayoutProps) {
  const isRegister = variant === "register";

  return (
    <main className="auth-page">
      <aside className="auth-brand-panel">
        <a
          className="auth-brand"
          href="/login"
          aria-label="Finance Tracker home"
        >
          <span className="auth-brand-mark" aria-hidden="true">
            F
          </span>
          <span>Finance Tracker</span>
        </a>

        <div className="auth-brand-content">
          <p className="auth-eyebrow">
            {isRegister
              ? "YOUR FINANCES, ORGANIZED"
              : "WELCOME TO FINANCE TRACKER"}
          </p>

          <h1>
            {isRegister ? (
              <>
                Build better
                <br />
                <span>financial habits.</span>
              </>
            ) : (
              <>
                Your money,
                <br />
                <span>in perspective.</span>
              </>
            )}
          </h1>

          <p className="auth-brand-description">
            {isRegister
              ? "Start organizing your finances and take control of your money."
              : "Understand your spending, plan your budgets, and stay in control of your finances."}
          </p>

          {isRegister ? (
            <ul className="auth-benefits">
              {registrationBenefits.map((benefit) => (
                <li key={benefit}>
                  <span className="auth-benefit-check" aria-hidden="true">
                    ✓
                  </span>
                  {benefit}
                </li>
              ))}
            </ul>
          ) : (
            <div className="auth-balance-card">
              <p className="auth-balance-label">YOUR FINANCES, AT A GLANCE</p>

              <p className="auth-balance-value">One clear picture.</p>

              <div className="auth-balance-divider" />

              <div className="auth-balance-row">
                <span>Income and expenses</span>
                <span className="auth-benefit-check" aria-hidden="true">
                  ✓
                </span>
              </div>

              <div className="auth-balance-row">
                <span>Budgets and goals</span>
                <span className="auth-benefit-check" aria-hidden="true">
                  ✓
                </span>
              </div>

              <div className="auth-balance-row">
                <span>Investments</span>
                <span className="auth-benefit-check" aria-hidden="true">
                  ✓
                </span>
              </div>
            </div>
          )}
        </div>
      </aside>

      <section className="auth-form-panel">
        <div className="auth-mobile-brand">
          <span className="auth-brand-mark" aria-hidden="true">
            F
          </span>
          <span>Finance Tracker</span>
        </div>

        <div className="auth-form-container">{children}</div>
      </section>
    </main>
  );
}

export default AuthLayout;
