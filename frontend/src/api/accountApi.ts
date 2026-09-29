import type { Account, AccountType } from "../types/account";

type CreateAccountRequest = {
  name: string;
  type: AccountType;
  currency: string;
  openingBalance: number;
};

type UpdateAccountRequest = {
  name: string;
  type: AccountType;
  currency: string;
};

export async function getAccounts(accessToken: string): Promise<Account[]> {
  const response = await fetch("http://localhost:8080/api/accounts", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    throw new Error("Failed to fetch accounts.");
  }

  return response.json();
}

export async function createAccount(
  accessToken: string,
  request: CreateAccountRequest,
): Promise<Account> {
  const response = await fetch("http://localhost:8080/api/accounts", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error("Failed to create account.");
  }

  return response.json();
}

export async function updateAccount(
  accessToken: string,
  accountId: string,
  request: UpdateAccountRequest,
): Promise<Account> {
  const response = await fetch(
    `http://localhost:8080/api/accounts/${accountId}`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    },
  );

  if (!response.ok) {
    throw new Error("Failed to update account.");
  }

  return response.json();
}

export async function deleteAccount(
  accessToken: string,
  accountId: string,
): Promise<void> {
  const response = await fetch(
    `http://localhost:8080/api/accounts/${accountId}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );

  if (!response.ok) {
    throw new Error("Failed to delete account.");
  }
}
