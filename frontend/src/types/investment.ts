export type InvestmentType = "STOCK" | "MUTUAL_FUND" | "ETF" | "GOLD" | "OTHER";

export type InvestmentTransactionType = "BUY" | "SELL";

export type Investment = {
  id: string;
  name: string;
  type: InvestmentType;
  quantity: number | null;
  averagePurchasePrice: number | null;
  currentValue: number;
  createdAt: string;
  updatedAt: string;
};

export type InvestmentTransaction = {
  id: string;
  investmentId: string;
  accountId: string;
  type: InvestmentTransactionType;
  amount: number;
  quantity: number | null;
  pricePerUnit: number | null;
  occurredAt: string;
  createdAt: string;
};

export type Portfolio = {
  totalInvested: number;
  currentValue: number;
  gainLoss: number;
  returnPercentage: number;
  investments: Investment[];
};
