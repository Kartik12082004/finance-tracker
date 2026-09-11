package com.kartik.finance_tracker.accounts;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kartik.finance_tracker.transactions.Transaction;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;

@Service
public class AccountBalanceService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountBalanceService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public BigDecimal calculateBalance(UUID accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<Transaction> transactions =
                transactionRepository.findAllByAccount_IdOrDestinationAccount_Id(
                        accountId,
                        accountId
                );

        BigDecimal balance = account.getOpeningBalance();

        for (Transaction transaction : transactions) {

            if (transaction.getType() == TransactionType.TRANSFER) {

                if (transaction.getAccount().getId().equals(accountId)) {
                    balance = balance.subtract(transaction.getAmount());
                }

                if (transaction.getDestinationAccount() != null
                        && transaction.getDestinationAccount().getId().equals(accountId)) {

                    if (account.getType() == AccountType.CREDIT_CARD) {
                        balance = balance.subtract(transaction.getAmount());
                    } else {
                        balance = balance.add(transaction.getAmount());
                    }
                }

            } else if (account.getType() == AccountType.CREDIT_CARD) {

                if (transaction.getType() == TransactionType.EXPENSE) {
                    balance = balance.add(transaction.getAmount());
                }

                if (transaction.getType() == TransactionType.INCOME) {
                    balance = balance.subtract(transaction.getAmount());
                }

            } else {

                if (transaction.getType() == TransactionType.INCOME) {
                    balance = balance.add(transaction.getAmount());
                }

                if (transaction.getType() == TransactionType.EXPENSE) {
                    balance = balance.subtract(transaction.getAmount());
                }
            }
        }

        return balance;
    }
}