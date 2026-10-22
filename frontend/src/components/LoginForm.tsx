import { useState } from "react";
import { login } from "../api/authApi";
import type { AuthResponse } from "../types/auth";

type LoginFormProps = {
  onLogin: (authResponse: AuthResponse) => void;
  onSwitchToRegister: () => void;
};

function LoginForm({ onLogin, onSwitchToRegister }: LoginFormProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleOAuthLogin(provider: "google" | "github") {
    window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
  }

  function validateEmail(value: string): string {
    const trimmedEmail = value.trim();

    if (!trimmedEmail) return "Email is required.";
    if (trimmedEmail.length > 255) {
      return "Email must be 255 characters or fewer.";
    }

    const emailPattern =
      /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/;

    return emailPattern.test(trimmedEmail)
      ? ""
      : "Enter a valid email address, such as name@example.com.";
  }

  function validatePassword(value: string): string {
    if (!value) return "Password is required.";
    if (value.length < 8) {
      return "Password must be at least 8 characters.";
    }
    return "";
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting) return;

    setError("");

    const trimmedEmail = email.trim();
    const nextEmailError = validateEmail(trimmedEmail);
    const nextPasswordError = validatePassword(password);

    setEmailError(nextEmailError);
    setPasswordError(nextPasswordError);

    if (nextEmailError || nextPasswordError) return;

    setIsSubmitting(true);

    try {
      const authResponse = await login({
        email: trimmedEmail,
        password,
      });
      onLogin(authResponse);
    } catch (caughtError: unknown) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "Invalid email or password.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <h2>Welcome Back</h2>
      <p>Sign in to access your Finance Tracker.</p>

      <div className="oauth-buttons">
        <button
          type="button"
          className="oauth-button oauth-google"
          onClick={() => handleOAuthLogin("google")}
          disabled={isSubmitting}
        >
          <svg viewBox="0 0 48 48" aria-hidden="true" className="oauth-icon">
            <path
              fill="#EA4335"
              d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
            />
            <path
              fill="#4285F4"
              d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.72 7.18l7.65 5.94c4.47-4.13 7.11-10.2 7.11-17.59z"
            />
            <path
              fill="#FBBC05"
              d="M10.53 28.59a14.4 14.4 0 0 1 0-9.18l-7.98-6.19a24 24 0 0 0 0 21.56l7.98-6.19z"
            />
            <path
              fill="#34A853"
              d="M24 48c6.48 0 11.93-2.13 15.9-5.81l-7.65-5.94c-2.13 1.43-4.85 2.27-8.25 2.27-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
            />
          </svg>
          Continue with Google
        </button>

        <button
          type="button"
          className="oauth-button oauth-github"
          onClick={() => handleOAuthLogin("github")}
          disabled={isSubmitting}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true" className="oauth-icon">
            <path
              fill="currentColor"
              d="M12 .9a11.1 11.1 0 0 0-3.51 21.63c.55.1.76-.24.76-.53v-2.08c-3.1.67-3.75-1.32-3.75-1.32-.5-1.29-1.24-1.63-1.24-1.63-1.01-.69.08-.68.08-.68 1.12.08 1.71 1.15 1.71 1.15.99 1.7 2.6 1.21 3.23.92.1-.72.39-1.21.7-1.49-2.48-.28-5.09-1.24-5.09-5.52 0-1.22.44-2.22 1.15-3-.12-.28-.5-1.42.11-2.96 0 0 .94-.3 3.06 1.15a10.6 10.6 0 0 1 5.57 0c2.12-1.45 3.06-1.15 3.06-1.15.61 1.54.23 2.68.11 2.96.72.78 1.15 1.78 1.15 3 0 4.29-2.61 5.23-5.1 5.51.4.35.75 1.03.75 2.08V22c0 .29.2.63.77.52A11.1 11.1 0 0 0 12 .9z"
            />
          </svg>
          Continue with GitHub
        </button>
      </div>

      <div className="auth-divider">
        <span>OR CONTINUE WITH EMAIL</span>
      </div>

      <label htmlFor="login-email">Email</label>
      <input
        id="login-email"
        name="email"
        type="email"
        autoComplete="email"
        value={email}
        maxLength={255}
        aria-invalid={Boolean(emailError)}
        aria-describedby={emailError ? "login-email-error" : undefined}
        disabled={isSubmitting}
        onChange={(event) => {
          setEmail(event.target.value);
          setEmailError("");
          setError("");
        }}
      />
      {emailError && (
        <p id="login-email-error" className="field-error" role="alert">
          {emailError}
        </p>
      )}

      <label htmlFor="login-password">Password</label>
      <div className="password-input-wrapper">
        <input
          id="login-password"
          name="password"
          type={showPassword ? "text" : "password"}
          autoComplete="current-password"
          value={password}
          aria-invalid={Boolean(passwordError)}
          aria-describedby={passwordError ? "login-password-error" : undefined}
          disabled={isSubmitting}
          onChange={(event) => {
            setPassword(event.target.value);
            setPasswordError("");
            setError("");
          }}
        />

        <button
          type="button"
          className="password-toggle"
          onClick={() => setShowPassword((current) => !current)}
          disabled={isSubmitting}
          aria-label={showPassword ? "Hide password" : "Show password"}
          aria-pressed={showPassword}
        >
          {showPassword ? (
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M3 3l18 18" />
              <path d="M10.6 10.6a2 2 0 002.8 2.8" />
              <path d="M9.9 5.2A11 11 0 0121 12a11.8 11.8 0 01-3.1 3.9" />
              <path d="M6.2 6.2A12 12 0 003 12a11 11 0 0018 0" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          )}
        </button>
      </div>
      {passwordError && (
        <p id="login-password-error" className="field-error" role="alert">
          {passwordError}
        </p>
      )}

      {error && (
        <p className="auth-error" role="alert" aria-live="assertive">
          {error}
        </p>
      )}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Signing in…" : "Login"}
      </button>

      <p>
        Don't have an account?{" "}
        <button
          type="button"
          onClick={onSwitchToRegister}
          disabled={isSubmitting}
        >
          Create Account
        </button>
      </p>
    </form>
  );
}

export default LoginForm;
