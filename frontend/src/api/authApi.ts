import type { AuthResponse } from "../types/auth";

type LoginRequest = {
  email: string;
  password: string;
};

type RefreshTokenRequest = {
  refreshToken: string;
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

export async function refreshToken(
  request: RefreshTokenRequest,
): Promise<AuthResponse> {
  const response = await fetch("http://localhost:8080/api/auth/refresh", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error("Token refresh failed.");
  }

  return response.json();
}
