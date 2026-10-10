import type { TransactionType } from "./transaction";

export type RecurringFrequencyUnit = "WEEK" | "MONTH" | "YEAR";

export type RecurringTransaction = {
  id: string;
  accountId: string;
  categoryId: string;
  categoryName: string;
  type: TransactionType;
  amount: number;
  description: string | null;
  frequencyUnit: RecurringFrequencyUnit;
  frequencyInterval: number;
  nextOccurrence: string;
  skipNextOccurrence: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateRecurringTransactionRequest = {
  accountId: string;
  categoryId: string;
  type: TransactionType;
  amount: number;
  description: string;
  frequencyUnit: RecurringFrequencyUnit;
  frequencyInterval: number;
  nextOccurrence: string;
};

export type UpdateRecurringTransactionRequest =
  CreateRecurringTransactionRequest;
