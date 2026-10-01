import { apiRequest } from "./apiClient";

export type AccountBalance = {
  accountId: string;
  balance: number;
};

export async function getAccountBalances(): Promise<AccountBalance[]> {
  return apiRequest<AccountBalance[]>(
    "http://localhost:8080/api/analytics/account-balances",
  );
}
