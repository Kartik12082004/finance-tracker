package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.transactions.Transaction;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;

class AccountBalanceServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private AccountBalanceService accountBalanceService;

    private User user;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);

        accountBalanceService = new AccountBalanceService(
                accountRepository,
                transactionRepository
        );

        user = new User(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );
    }

    @Test
    void calculateBalance_shouldReturnOpeningBalance_whenNoTransactionsExist() {

        Account account = account(
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        mockAccount(account);
        mockTransactions(account);

        BigDecimal balance =
                accountBalanceService.calculateBalance(account.getId());

        assertThat(balance).isEqualByComparingTo("10000.00");
    }

    @Test
    void calculateBalance_shouldAddIncomeAndSubtractExpense_forBankAccount() {

        Account account = account(
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        Transaction income = transaction(
                account,
                null,
                TransactionType.INCOME,
                new BigDecimal("5000.00")
        );

        Transaction expense = transaction(
                account,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("2000.00")
        );

        mockAccount(account);
        mockTransactions(account, income, expense);

        BigDecimal balance =
                accountBalanceService.calculateBalance(account.getId());

        assertThat(balance).isEqualByComparingTo("13000.00");
    }

    @Test
    void calculateBalance_shouldHandleTransferForBankAccounts() {

        Account source = account(
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        Account destination = account(
                AccountType.BANK,
                new BigDecimal("5000.00")
        );

        Transaction outgoingTransfer = transaction(
                source,
                destination,
                TransactionType.TRANSFER,
                new BigDecimal("3000.00")
        );

        Transaction incomingTransfer = transaction(
                destination,
                source,
                TransactionType.TRANSFER,
                new BigDecimal("2000.00")
        );

        mockAccount(source);
        when(transactionRepository
                .findAllByAccount_IdOrDestinationAccount_Id(
                        source.getId(),
                        source.getId()
                ))
                .thenReturn(List.of(outgoingTransfer, incomingTransfer));

        BigDecimal balance =
                accountBalanceService.calculateBalance(source.getId());

        assertThat(balance).isEqualByComparingTo("9000.00");
    }

    @Test
    void calculateBalance_shouldIncreaseDebt_whenCreditCardHasExpense() {

        Account account = account(
                AccountType.CREDIT_CARD,
                BigDecimal.ZERO
        );

        Transaction expense = transaction(
                account,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("2500.00")
        );

        mockAccount(account);
        mockTransactions(account, expense);

        BigDecimal balance =
                accountBalanceService.calculateBalance(account.getId());

        assertThat(balance).isEqualByComparingTo("2500.00");
    }

    @Test
    void calculateBalance_shouldReduceDebt_whenCreditCardReceivesPayment() {

        Account creditCard = account(
                AccountType.CREDIT_CARD,
                BigDecimal.ZERO
        );

        Account bankAccount = account(
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        Transaction payment = transaction(
                bankAccount,
                creditCard,
                TransactionType.TRANSFER,
                new BigDecimal("3000.00")
        );

        mockAccount(creditCard);
        mockTransactions(creditCard, payment);

        BigDecimal balance =
                accountBalanceService.calculateBalance(creditCard.getId());

        assertThat(balance).isEqualByComparingTo("-3000.00");
    }

    @Test
    void calculateBalance_shouldReduceCreditCardDebt_whenRefundIsIncome() {

        Account account = account(
                AccountType.CREDIT_CARD,
                BigDecimal.ZERO
        );

        Transaction refund = transaction(
                account,
                null,
                TransactionType.INCOME,
                new BigDecimal("1000.00")
        );

        mockAccount(account);
        mockTransactions(account, refund);

        BigDecimal balance =
                accountBalanceService.calculateBalance(account.getId());

        assertThat(balance).isEqualByComparingTo("-1000.00");
    }

    private Account account(
            AccountType type,
            BigDecimal openingBalance
    ) {
        Account account = new Account(
                user,
                "Test Account",
                type,
                "INR"
        );

        account.setOpeningBalance(openingBalance);

        return account;
    }

    private Transaction transaction(
            Account account,
            Account destinationAccount,
            TransactionType type,
            BigDecimal amount
    ) {
        return new Transaction(
                user,
                account,
                destinationAccount,
                null,
                type,
                amount,
                "Test transaction",
                java.time.OffsetDateTime.now()
        );
    }

    private void mockAccount(Account account) {
        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.of(account));
    }

    private void mockTransactions(
            Account account,
            Transaction... transactions
    ) {
        when(transactionRepository
                .findAllByAccount_IdOrDestinationAccount_Id(
                        account.getId(),
                        account.getId()
                ))
                .thenReturn(List.of(transactions));
    }
}