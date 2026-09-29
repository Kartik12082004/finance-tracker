import { refreshToken } from "./authApi";
import {
  clearAuthSession,
  getAuthSession,
  saveAuthSession,
} from "../auth/authStorage";

async function sendRequest(
  url: string,
  options: RequestInit,
  accessToken: string,
): Promise<Response> {
  const headers = new Headers(options.headers);

  headers.set("Authorization", `Bearer ${accessToken}`);

  return fetch(url, {
    ...options,
    headers,
  });
}

export async function apiRequest<T>(
  url: string,
  options: RequestInit = {},
): Promise<T> {
  const authSession = getAuthSession();

  if (authSession === null) {
    throw new Error("User is not authenticated.");
  }

  let response = await sendRequest(url, options, authSession.accessToken);

  if (response.status !== 401) {
    if (!response.ok) {
      throw new Error("API request failed.");
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  }

  // The access token is no longer accepted.
  // Try to rotate it using the stored refresh token.
  try {
    const refreshedSession = await refreshToken({
      refreshToken: authSession.refreshToken,
    });

    // The backend rotates the refresh token, so replace the entire
    // authentication session rather than keeping the old token.
    saveAuthSession(refreshedSession);

    // Retry the original request exactly once with the new access token.
    response = await sendRequest(url, options, refreshedSession.accessToken);
  } catch {
    // The refresh token is invalid, expired, or otherwise unusable.
    // Remove the local session so stale credentials are not retained.
    clearAuthSession();

    throw new Error("Authentication session expired.");
  }

  if (!response.ok) {
    throw new Error("API request failed.");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
