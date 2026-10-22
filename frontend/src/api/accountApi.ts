import { apiRequest } from "./apiClient";
import type { Account, AccountType } from "../types/account";

type CreateAccountRequest = {
  name: string;
  type: AccountType;
  currency: string;
  openingBalance: number;
  creditLimit: number | null;
  parentAccountId: string | null;
};

type UpdateAccountRequest = {
  name: string;
  type: AccountType;
  currency: string;
  creditLimit: number | null;
  parentAccountId: string | null;
};

export async function getAccounts(): Promise<Account[]> {
  return apiRequest<Account[]>("http://localhost:8080/api/accounts");
}

export async function createAccount(
  request: CreateAccountRequest,
): Promise<Account> {
  return apiRequest<Account>("http://localhost:8080/api/accounts", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
}

export async function updateAccount(
  accountId: string,
  request: UpdateAccountRequest,
): Promise<Account> {
  return apiRequest<Account>(
    `http://localhost:8080/api/accounts/${accountId}`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    },
  );
}

export async function deleteAccount(accountId: string): Promise<void> {
  return apiRequest<void>(`http://localhost:8080/api/accounts/${accountId}`, {
    method: "DELETE",
  });
}
