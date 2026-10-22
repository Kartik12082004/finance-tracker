export type AccountType =
  | "BANK"
  | "CASH"
  | "CREDIT_CARD"
  | "INVESTMENT"
  | "GOAL";

export type Account = {
  id: string;
  name: string;
  type: AccountType;
  currency: string;
  openingBalance: number;
  creditLimit: number | null;
  parentAccountId: string | null;
  createdAt: string;
  updatedAt: string;
};
