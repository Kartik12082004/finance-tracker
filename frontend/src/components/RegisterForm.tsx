import { useState } from "react";
import { register } from "../api/authApi";
import type { AuthResponse } from "../types/auth";

type RegisterFormProps = {
  onRegister: (authResponse: AuthResponse) => void;
  onSwitchToLogin: () => void;
};

function RegisterForm({ onRegister, onSwitchToLogin }: RegisterFormProps) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError("");

    if (name.trim() === "") {
      setError("Name is required.");
      return;
    }

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
      const authResponse = await register({
        name: name.trim(),
        email: email.trim(),
        password,
      });

      onRegister(authResponse);
    } catch {
      setError("Registration failed. The email may already be registered.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>Create Account</h2>

      <label>
        Name:
        <input
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
        />
      </label>

      <br />

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
        {isSubmitting ? "Creating account..." : "Create Account"}
      </button>

      <p>
        Already have an account?{" "}
        <button type="button" onClick={onSwitchToLogin}>
          Login
        </button>
      </p>
    </form>
  );
}

export default RegisterForm;
