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
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleOAuthLogin(provider: "google" | "github") {
    window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      setError("Email is required.");
      return;
    }

    if (!password) {
      setError("Password is required.");
      return;
    }

    setIsSubmitting(true);

    try {
      const authResponse = await login({
        email: trimmedEmail,
        password,
      });

      onLogin(authResponse);
    } catch {
      setError("Login failed. Please check your email and password.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Welcome Back</h2>
      <p>Sign in to access your Finance Tracker.</p>

      <div className="oauth-buttons">
        <button
          type="button"
          className="oauth-button oauth-google"
          onClick={() => handleOAuthLogin("google")}
        >
          <span aria-hidden="true">G</span>
          Continue with Google
        </button>

        <button
          type="button"
          className="oauth-button oauth-github"
          onClick={() => handleOAuthLogin("github")}
        >
          <span aria-hidden="true">●</span>
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
        onChange={(event) => setEmail(event.target.value)}
        maxLength={255}
        required
        disabled={isSubmitting}
      />

      <label htmlFor="login-password">Password</label>
      <input
        id="login-password"
        name="password"
        type={showPassword ? "text" : "password"}
        autoComplete="current-password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        required
        disabled={isSubmitting}
      />

      <button
        type="button"
        onClick={() => setShowPassword((current) => !current)}
        disabled={isSubmitting}
        aria-label={showPassword ? "Hide password" : "Show password"}
      >
        {showPassword ? "Hide password" : "Show password"}
      </button>

      {error && (
        <p role="alert" aria-live="polite">
          {error}
        </p>
      )}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Logging in..." : "Login"}
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
