package com.kartik.finance_tracker.investments;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentTransactionRepository
        extends JpaRepository<InvestmentTransaction, UUID> {

    List<InvestmentTransaction> findByInvestment_Id(UUID investmentId);

    List<InvestmentTransaction> findByUser_Id(UUID userId);

    List<InvestmentTransaction> findByAccount_Id(UUID accountId);

    boolean existsByInvestment_Id(UUID investmentId);
}
