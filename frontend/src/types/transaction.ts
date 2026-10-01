export type TransactionType = "INCOME" | "EXPENSE" | "TRANSFER";

export type TransactionStatus = "POSTED" | "VOIDED";

export type Transaction = {
  id: string;
  accountId: string;
  destinationAccountId: string | null;
  categoryId: string | null;
  type: TransactionType;
  status: TransactionStatus;
  amount: number;
  description: string | null;
  occurredAt: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateTransactionRequest = {
  accountId: string;
  destinationAccountId?: string;
  categoryId?: string;
  type: TransactionType;
  amount: number;
  description?: string;
  occurredAt: string;
};
