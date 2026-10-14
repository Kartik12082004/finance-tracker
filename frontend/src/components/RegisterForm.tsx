import { useState } from "react";
import { register } from "../api/authApi";
import type { AuthResponse } from "../types/auth";

type RegisterFormProps = {
  onRegister: (authResponse: AuthResponse) => void;
  onSwitchToLogin: () => void;
};

function RegisterForm({ onRegister, onSwitchToLogin }: RegisterFormProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleOAuthSignIn(provider: "google" | "github") {
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

    if (password.length < 8 || password.length > 100) {
      setError("Password must be between 8 and 100 characters.");
      return;
    }

    setIsSubmitting(true);

    try {
      const authResponse = await register({
        email: trimmedEmail,
        password,
      });

      onRegister(authResponse);
    } catch {
      setError(
        "Registration failed. Please check your details or try another email address.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <h2>Create Account</h2>
      <p>Create your Finance Tracker account to get started.</p>

      <div className="oauth-buttons">
        <button
          type="button"
          className="oauth-button oauth-google"
          onClick={() => handleOAuthSignIn("google")}
        >
          <span aria-hidden="true">G</span>
          Continue with Google
        </button>

        <button
          type="button"
          className="oauth-button oauth-github"
          onClick={() => handleOAuthSignIn("github")}
        >
          <span aria-hidden="true">●</span>
          Continue with GitHub
        </button>
      </div>

      <div className="auth-divider">
        <span>OR CONTINUE WITH EMAIL</span>
      </div>

      <label htmlFor="register-email">Email</label>
      <input
        id="register-email"
        name="email"
        type="email"
        autoComplete="email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        maxLength={255}
        required
        disabled={isSubmitting}
      />

      <label htmlFor="register-password">Password</label>
      <input
        id="register-password"
        name="new-password"
        type={showPassword ? "text" : "password"}
        autoComplete="new-password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        minLength={8}
        maxLength={100}
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
        {isSubmitting ? "Creating account..." : "Create Account"}
      </button>

      <p>
        Already have an account?{" "}
        <button type="button" onClick={onSwitchToLogin} disabled={isSubmitting}>
          Login
        </button>
      </p>
    </form>
  );
}

export default RegisterForm;
