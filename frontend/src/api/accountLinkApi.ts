import { apiRequest } from "./apiClient";

export type AccountLinkStatus = {
  googleLinked: boolean;
  githubLinked: boolean;
};

export async function getAccountLinkStatus(): Promise<AccountLinkStatus> {
  return apiRequest<AccountLinkStatus>(
    "http://localhost:8080/api/account-links",
  );
}

export async function initiateGitHubAccountLink(): Promise<void> {
  await apiRequest<void>("http://localhost:8080/api/account-links/github", {
    method: "POST",
    credentials: "include",
  });
}

export async function initiateGoogleAccountLink(): Promise<void> {
  await apiRequest<void>("http://localhost:8080/api/account-links/google", {
    method: "POST",
    credentials: "include",
  });
}

export async function unlinkGitHubAccount(): Promise<void> {
  await apiRequest<void>("http://localhost:8080/api/account-links/github", {
    method: "DELETE",
    credentials: "include",
  });
}

export async function unlinkGoogleAccount(): Promise<void> {
  await apiRequest<void>("http://localhost:8080/api/account-links/google", {
    method: "DELETE",
    credentials: "include",
  });
}
