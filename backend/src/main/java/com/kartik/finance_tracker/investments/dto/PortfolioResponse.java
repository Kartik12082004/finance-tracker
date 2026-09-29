package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal gainLoss,
        BigDecimal returnPercentage,
        List<InvestmentResponse> investments
) {
}
