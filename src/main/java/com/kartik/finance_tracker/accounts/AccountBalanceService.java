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

        // A transaction can affect an account either as the source account
        // or as the destination account of a transfer.
        List<Transaction> transactions =
                transactionRepository.findAllByAccount_IdOrDestinationAccount_Id(
                        accountId,
                        accountId
                );

        BigDecimal balance = account.getOpeningBalance();

        for (Transaction transaction : transactions) {

            if (transaction.getType() == TransactionType.TRANSFER) {

                // Money leaving the account decreases its balance.
                if (transaction.getAccount().getId().equals(accountId)) {
                    balance = balance.subtract(transaction.getAmount());
                }

                if (transaction.getDestinationAccount() != null
                        && transaction.getDestinationAccount().getId().equals(accountId)) {

                    /*
                     * Credit-card balances use a different convention:
                     *  0      = no outstanding debt
                     *  +5000  = 5000 owed
                     *  -5000  = 5000 credit/overpayment
                     *
                     * Therefore, a payment into a credit card reduces its
                     * balance instead of increasing it.
                     */
                    if (account.getType() == AccountType.CREDIT_CARD) {
                        balance = balance.subtract(transaction.getAmount());
                    } else {
                        // Receiving money increases a normal asset account.
                        balance = balance.add(transaction.getAmount());
                    }
                }

            } else if (account.getType() == AccountType.CREDIT_CARD) {

                /*
                 * Credit-card spending creates/increases debt, so an expense
                 * increases the balance instead of decreasing it.
                 */
                if (transaction.getType() == TransactionType.EXPENSE) {
                    balance = balance.add(transaction.getAmount());
                }

                // Income/payment toward a credit card reduces the amount owed.
                if (transaction.getType() == TransactionType.INCOME) {
                    balance = balance.subtract(transaction.getAmount());
                }

            } else {

                // For normal asset accounts, income increases the balance.
                if (transaction.getType() == TransactionType.INCOME) {
                    balance = balance.add(transaction.getAmount());
                }

                // For normal asset accounts, expenses decrease the balance.
                if (transaction.getType() == TransactionType.EXPENSE) {
                    balance = balance.subtract(transaction.getAmount());
                }
            }
        }

        return balance;
    }
}