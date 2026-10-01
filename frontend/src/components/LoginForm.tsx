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
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError("");

    if (email.trim() === "") {
      setError("Email is required.");
      return;
    }

    if (password.trim() === "") {
      setError("Password is required.");
      return;
    }

    setIsSubmitting(true);

    try {
      const authResponse = await login({
        email,
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
    <form onSubmit={handleSubmit}>
      <h2>Login</h2>

      <label>
        Email:
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
      </label>

      <br />

      <label>
        Password:
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </label>

      <br />

      {error && <p>{error}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Logging in..." : "Login"}
      </button>

      <p>
        Don't have an account?{" "}
        <button type="button" onClick={onSwitchToRegister}>
          Create Account
        </button>
      </p>
    </form>
  );
}

export default LoginForm;
