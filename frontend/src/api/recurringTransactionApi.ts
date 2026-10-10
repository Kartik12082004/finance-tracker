import { apiRequest } from "./apiClient";
import type {
  CreateRecurringTransactionRequest,
  RecurringTransaction,
  UpdateRecurringTransactionRequest,
} from "../types/recurringTransaction";

export async function getRecurringTransactions(): Promise<
  RecurringTransaction[]
> {
  return apiRequest<RecurringTransaction[]>(
    "http://localhost:8080/api/recurring-transactions",
  );
}

export async function createRecurringTransaction(
  request: CreateRecurringTransactionRequest,
): Promise<RecurringTransaction> {
  return apiRequest<RecurringTransaction>(
    "http://localhost:8080/api/recurring-transactions",
    {
      method: "POST",
      body: JSON.stringify(request),
    },
  );
}

export async function updateRecurringTransaction(
  recurringTransactionId: string,
  request: UpdateRecurringTransactionRequest,
): Promise<RecurringTransaction> {
  return apiRequest<RecurringTransaction>(
    `http://localhost:8080/api/recurring-transactions/${recurringTransactionId}`,
    {
      method: "PUT",
      body: JSON.stringify(request),
    },
  );
}

export async function skipNextRecurringTransaction(
  recurringTransactionId: string,
): Promise<RecurringTransaction> {
  return apiRequest<RecurringTransaction>(
    `http://localhost:8080/api/recurring-transactions/${recurringTransactionId}/skip-next`,
    {
      method: "PATCH",
    },
  );
}

export async function undoSkipNextRecurringTransaction(
  recurringTransactionId: string,
): Promise<RecurringTransaction> {
  return apiRequest<RecurringTransaction>(
    `http://localhost:8080/api/recurring-transactions/${recurringTransactionId}/undo-skip`,
    {
      method: "PATCH",
    },
  );
}

export async function deleteRecurringTransaction(
  recurringTransactionId: string,
): Promise<void> {
  await apiRequest<void>(
    `http://localhost:8080/api/recurring-transactions/${recurringTransactionId}`,
    {
      method: "DELETE",
    },
  );
}
