import { refreshToken } from "./authApi";
import {
  clearAuthSession,
  getAuthSession,
  saveAuthSession,
} from "../auth/authStorage";

let refreshPromise: Promise<string | null> | null = null;

async function sendRequest(
  url: string,
  options: RequestInit,
  accessToken: string,
): Promise<Response> {
  const headers = new Headers(options.headers);

  headers.set("Authorization", `Bearer ${accessToken}`);

  if (options.body !== undefined && options.body !== null) {
    if (!headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
  }

  return fetch(url, {
    ...options,
    headers,
  });
}

async function refreshAccessToken(
  refreshTokenValue: string,
): Promise<string | null> {
  if (refreshPromise !== null) {
    return refreshPromise;
  }

  refreshPromise = (async () => {
    try {
      const refreshedSession = await refreshToken({
        refreshToken: refreshTokenValue,
      });

      saveAuthSession(refreshedSession);

      return refreshedSession.accessToken;
    } catch {
      clearAuthSession();

      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
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
  // Only one refresh request is allowed at a time.
  // Any other requests that receive 401 will wait for the same refresh.
  const newAccessToken = await refreshAccessToken(authSession.refreshToken);

  if (newAccessToken === null) {
    throw new Error("Authentication session expired.");
  }

  // Retry the original request exactly once with the new access token.
  response = await sendRequest(url, options, newAccessToken);

  if (!response.ok) {
    throw new Error("API request failed.");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
