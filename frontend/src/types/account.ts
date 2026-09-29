export type AccountType = "BANK" | "CASH" | "CREDIT_CARD" | "INVESTMENT";

export type Account = {
  id: string;
  name: string;
  type: AccountType;
  currency: string;
  openingBalance: number;
  createdAt: string;
  updatedAt: string;
};
