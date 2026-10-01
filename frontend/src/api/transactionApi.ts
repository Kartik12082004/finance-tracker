import { apiRequest } from "./apiClient";
import type {
  CreateTransactionRequest,
  Transaction,
} from "../types/transaction";

export async function getTransactions(): Promise<Transaction[]> {
  return apiRequest<Transaction[]>("http://localhost:8080/api/transactions");
}

export async function createTransaction(
  request: CreateTransactionRequest,
): Promise<Transaction> {
  return apiRequest<Transaction>("http://localhost:8080/api/transactions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
}

export async function voidTransaction(
  transactionId: string,
): Promise<Transaction> {
  return apiRequest<Transaction>(
    `http://localhost:8080/api/transactions/${transactionId}/void`,
    {
      method: "POST",
    },
  );
}
