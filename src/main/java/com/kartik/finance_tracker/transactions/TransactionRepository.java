package com.kartik.finance_tracker.transactions;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByUser_Id(UUID userId);

    List<Transaction> findAllByAccount_IdOrDestinationAccount_Id(
            UUID accountId,
            UUID destinationAccountId
    );
}