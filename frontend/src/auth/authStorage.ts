import type { AuthResponse } from "../types/auth";

const AUTH_STORAGE_KEY = "finance_tracker_auth";

export function saveAuthSession(authResponse: AuthResponse): void {
  sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse));
}

export function getAuthSession(): AuthResponse | null {
  const storedAuth = sessionStorage.getItem(AUTH_STORAGE_KEY);

  if (storedAuth === null) {
    return null;
  }

  try {
    return JSON.parse(storedAuth) as AuthResponse;
  } catch {
    // Remove corrupted session data instead of leaving the app
    // in an unusable authentication state.
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function clearAuthSession(): void {
  sessionStorage.removeItem(AUTH_STORAGE_KEY);
}
