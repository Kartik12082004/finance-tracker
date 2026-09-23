package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.investments.Investment;
import com.kartik.finance_tracker.investments.InvestmentTransaction;
import com.kartik.finance_tracker.investments.InvestmentTransactionRepository;
import com.kartik.finance_tracker.investments.InvestmentTransactionType;
import com.kartik.finance_tracker.investments.InvestmentType;
import com.kartik.finance_tracker.transactions.Transaction;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;

class AccountBalanceServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private InvestmentTransactionRepository investmentTransactionRepository;
    private AccountBalanceService accountBalanceService;

    private User user;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        investmentTransactionRepository = mock(InvestmentTransactionRepository.class);

        accountBalanceService = new AccountBalanceService(
                accountRepository,
                transactionRepository,
                investmentTransactionRepository
        );

        // All test accounts and transactions belong to the same test user.
        user = new User(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );

        // Most existing tests do not contain investment transactions.
        // Return an empty list unless a test explicitly adds investment activity.
        when(investmentTransactionRepository.findByAccount_Id(any()))
                .thenReturn(List.of());
    }

    @Test
    void calculateBalance_shouldReturnOpeningBalance_whenNoTransactionsExist() {

        // With no transactions, the current balance should equal the opening balance.
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

        // Normal asset accounts increase when money comes in
        // and decrease when money is spent.
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

        // 10,000 opening + 5,000 income - 2,000 expense = 13,000.
        assertThat(balance).isEqualByComparingTo("13000.00");
    }

    @Test
    void calculateBalance_shouldHandleTransferForBankAccounts() {

        // A transfer is neither income nor expense.
        // It decreases the source account and increases the destination account.
        Account source = account(
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        Account destination = account(
                AccountType.BANK,
                new BigDecimal("5000.00")
        );

        // 3,000 leaves the source account.
        Transaction outgoingTransfer = transaction(
                source,
                destination,
                TransactionType.TRANSFER,
                new BigDecimal("3000.00")
        );

        // 2,000 is transferred back into the source account.
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

        // 10,000 opening - 3,000 outgoing + 2,000 incoming = 9,000.
        assertThat(balance).isEqualByComparingTo("9000.00");
    }

    @Test
    void calculateBalance_shouldIncreaseDebt_whenCreditCardHasExpense() {

        // Credit cards use a different balance convention:
        // 0 means no debt, while a positive balance represents money owed.
        Account account = account(
                AccountType.CREDIT_CARD,
                BigDecimal.ZERO
        );

        // Spending on a credit card increases the amount owed.
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

        // The card now has 2,500 of outstanding debt.
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

        // A payment is represented as a transfer from the bank account
        // into the credit card account.
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

        // The payment creates a 3,000 credit because the card had no debt.
        // A negative credit-card balance represents an overpayment/credit.
        assertThat(balance).isEqualByComparingTo("-3000.00");
    }

    @Test
    void calculateBalance_shouldReduceCreditCardDebt_whenRefundIsIncome() {

        // A refund is represented as income for the credit card.
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

        // A refund reduces the amount owed and can create a card credit.
        assertThat(balance).isEqualByComparingTo("-1000.00");
    }

    @Test
    void calculateBalance_shouldSubtractInvestmentBuy() {

        Account account = account(
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        Investment investment = new Investment(
                user,
                "Nifty 50 ETF",
                InvestmentType.ETF
        );

        InvestmentTransaction buy = new InvestmentTransaction(
                user,
                investment,
                account,
                InvestmentTransactionType.BUY,
                new BigDecimal("3000.00"),
                new BigDecimal("10"),
                new BigDecimal("300.00"),
                java.time.OffsetDateTime.now()
        );

        mockAccount(account);
        mockTransactions(account);

        when(investmentTransactionRepository.findByAccount_Id(account.getId()))
                .thenReturn(List.of(buy));

        BigDecimal balance =
                accountBalanceService.calculateBalance(account.getId());

        // 10,000 opening balance - 3,000 investment purchase = 7,000.
        assertThat(balance).isEqualByComparingTo("7000.00");
    }

    @Test
    void calculateBalance_shouldAddInvestmentSell() {

        Account account = account(
                AccountType.BANK,
                new BigDecimal("5000.00")
        );

        Investment investment = new Investment(
                user,
                "Nifty 50 ETF",
                InvestmentType.ETF
        );

        InvestmentTransaction sell = new InvestmentTransaction(
                user,
                investment,
                account,
                InvestmentTransactionType.SELL,
                new BigDecimal("2000.00"),
                new BigDecimal("5"),
                new BigDecimal("400.00"),
                java.time.OffsetDateTime.now()
        );

        mockAccount(account);
        mockTransactions(account);

        when(investmentTransactionRepository.findByAccount_Id(account.getId()))
                .thenReturn(List.of(sell));

        BigDecimal balance =
                accountBalanceService.calculateBalance(account.getId());

        // 5,000 opening balance + 2,000 investment sale proceeds = 7,000.
        assertThat(balance).isEqualByComparingTo("7000.00");
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
