import { apiRequest } from "./apiClient";
import type {
  Investment,
  InvestmentTransaction,
  InvestmentType,
  Portfolio,
} from "../types/investment";

type CreateInvestmentRequest = {
  name: string;
  type: InvestmentType;
};

type UpdateInvestmentRequest = {
  name: string;
  type: InvestmentType;
  currentValue: number;
};

type BuyInvestmentRequest = {
  accountId: string;
  amount: number;
  quantity?: number;
  pricePerUnit?: number;
  occurredAt: string;
};

type SellInvestmentRequest = {
  accountId: string;
  amount: number;
  quantity?: number;
  pricePerUnit?: number;
  occurredAt: string;
};

export async function getInvestments(): Promise<Investment[]> {
  return apiRequest<Investment[]>("http://localhost:8080/api/investments");
}

export async function getPortfolio(): Promise<Portfolio> {
  return apiRequest<Portfolio>(
    "http://localhost:8080/api/investments/portfolio",
  );
}

export async function createInvestment(
  request: CreateInvestmentRequest,
): Promise<Investment> {
  return apiRequest<Investment>("http://localhost:8080/api/investments", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
}

export async function updateInvestment(
  investmentId: string,
  request: UpdateInvestmentRequest,
): Promise<Investment> {
  return apiRequest<Investment>(
    `http://localhost:8080/api/investments/${investmentId}`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    },
  );
}

export async function deleteInvestment(investmentId: string): Promise<void> {
  return apiRequest<void>(
    `http://localhost:8080/api/investments/${investmentId}`,
    {
      method: "DELETE",
    },
  );
}

export async function buyInvestment(
  investmentId: string,
  request: BuyInvestmentRequest,
): Promise<InvestmentTransaction> {
  return apiRequest<InvestmentTransaction>(
    `http://localhost:8080/api/investments/${investmentId}/buys`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    },
  );
}

export async function sellInvestment(
  investmentId: string,
  request: SellInvestmentRequest,
): Promise<InvestmentTransaction> {
  return apiRequest<InvestmentTransaction>(
    `http://localhost:8080/api/investments/${investmentId}/sells`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    },
  );
}

export async function getInvestmentTransactions(
  investmentId: string,
): Promise<InvestmentTransaction[]> {
  return apiRequest<InvestmentTransaction[]>(
    `http://localhost:8080/api/investments/${investmentId}/transactions`,
  );
}
