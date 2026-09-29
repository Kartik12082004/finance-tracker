import type { AuthResponse } from "../types/auth";

type LoginRequest = {
  email: string;
  password: string;
};

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await fetch("http://localhost:8080/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error("Login failed.");
  }

  return response.json();
}
