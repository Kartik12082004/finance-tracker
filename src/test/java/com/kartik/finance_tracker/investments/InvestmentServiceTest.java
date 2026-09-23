package com.kartik.finance_tracker.investments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.investments.dto.BuyInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.CreateInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.SellInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.UpdateInvestmentRequest;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

class InvestmentServiceTest {

    private InvestmentRepository investmentRepository;
    private InvestmentTransactionRepository investmentTransactionRepository;
    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private AccountBalanceService accountBalanceService;

    private InvestmentService investmentService;

    private User user;
    private Account account;
    private Investment investment;

    @BeforeEach
    void setUp() {

        investmentRepository = mock(InvestmentRepository.class);
        investmentTransactionRepository = mock(InvestmentTransactionRepository.class);
        userRepository = mock(UserRepository.class);
        accountRepository = mock(AccountRepository.class);
        accountBalanceService = mock(AccountBalanceService.class);

        investmentService = new InvestmentService(
                investmentRepository,
                investmentTransactionRepository,
                userRepository,
                accountRepository,
                accountBalanceService
        );

        user = new User(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );

        account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        account.setOpeningBalance(new BigDecimal("100000.00"));

        investment = new Investment(
                user,
                "Nifty 50 ETF",
                InvestmentType.ETF
        );

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
    }

    @Test
    void createInvestment_shouldCreateHolding() {

        CreateInvestmentRequest request = new CreateInvestmentRequest(
                "Nifty 50 ETF",
                InvestmentType.ETF
        );

        when(investmentRepository.save(any(Investment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.createInvestment(
                user.getId(),
                request
        );

        assertThat(response.name()).isEqualTo("Nifty 50 ETF");
        assertThat(response.type()).isEqualTo(InvestmentType.ETF);
        assertThat(response.currentValue())
                .isEqualByComparingTo("0");

        verify(investmentRepository).save(any(Investment.class));
    }

    @Test
    void getInvestments_shouldReturnUsersInvestments() {

        when(investmentRepository.findByUser_Id(user.getId()))
                .thenReturn(List.of(investment));

        var response = investmentService.getInvestments(user.getId());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name())
                .isEqualTo("Nifty 50 ETF");
    }

    @Test
    void updateInvestment_shouldUpdateHoldingDetails() {

        UpdateInvestmentRequest request = new UpdateInvestmentRequest(
                "Updated ETF",
                InvestmentType.ETF,
                new BigDecimal("12500.00")
        );

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(investmentRepository.save(any(Investment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.updateInvestment(
                user.getId(),
                investment.getId(),
                request
        );

        assertThat(response.name()).isEqualTo("Updated ETF");
        assertThat(response.currentValue())
                .isEqualByComparingTo("12500.00");

        verify(investmentRepository).save(investment);
    }

    @Test
    void buy_shouldCreateInvestmentTransactionAndUpdateHolding() {

        BuyInvestmentRequest request = new BuyInvestmentRequest(
                account.getId(),
                new BigDecimal("3000.00"),
                new BigDecimal("10"),
                new BigDecimal("300.00"),
                OffsetDateTime.now()
        );

        // Account access is user-scoped to enforce account ownership.
        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(accountBalanceService.calculateBalance(account.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        when(investmentTransactionRepository.save(
                any(InvestmentTransaction.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        when(investmentRepository.save(any(Investment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.buy(
                user.getId(),
                investment.getId(),
                request
        );

        assertThat(response.type())
                .isEqualTo(InvestmentTransactionType.BUY);

        assertThat(response.amount())
                .isEqualByComparingTo("3000.00");

        assertThat(investment.getQuantity())
                .isEqualByComparingTo("10");

        assertThat(investment.getAveragePurchasePrice())
                .isEqualByComparingTo("300.00");

        verify(investmentTransactionRepository)
                .save(any(InvestmentTransaction.class));

        verify(investmentRepository)
                .save(investment);
    }

    @Test
    void buy_shouldRejectWhenAccountHasInsufficientFunds() {

        BuyInvestmentRequest request = new BuyInvestmentRequest(
                account.getId(),
                new BigDecimal("15000.00"),
                null,
                null,
                OffsetDateTime.now()
        );

        // Account access is user-scoped to enforce account ownership.
        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(accountBalanceService.calculateBalance(account.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        assertThatThrownBy(() ->
                investmentService.buy(
                        user.getId(),
                        investment.getId(),
                        request
                )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Insufficient funds");

        verify(investmentTransactionRepository, never())
                .save(any());

        verify(investmentRepository, never())
                .save(any());
    }

    @Test
    void buy_shouldRejectWhenQuantityAndPriceDoNotMatchAmount() {

        BuyInvestmentRequest request = new BuyInvestmentRequest(
                account.getId(),
                new BigDecimal("3000.00"),
                new BigDecimal("10"),
                new BigDecimal("250.00"),
                OffsetDateTime.now()
        );

        // Account access is user-scoped to enforce account ownership.
        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(accountBalanceService.calculateBalance(account.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        assertThatThrownBy(() ->
                investmentService.buy(
                        user.getId(),
                        investment.getId(),
                        request
                )
        )
        .isInstanceOf(IllegalArgumentException.class);

        verify(investmentTransactionRepository, never())
                .save(any());
    }

    @Test
    void sell_shouldCreateInvestmentTransactionAndReduceQuantity() {

        // Start with an existing holding of 10 units.
        investment.setQuantity(new BigDecimal("10"));
        investment.setAveragePurchasePrice(new BigDecimal("300.00"));

        SellInvestmentRequest request = new SellInvestmentRequest(
                account.getId(),
                new BigDecimal("1600.00"),
                new BigDecimal("5"),
                new BigDecimal("320.00"),
                OffsetDateTime.now()
        );

        // Account access is user-scoped to enforce account ownership.
        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(investmentTransactionRepository.save(
                any(InvestmentTransaction.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        when(investmentRepository.save(any(Investment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.sell(
                user.getId(),
                investment.getId(),
                request
        );

        assertThat(response.type())
                .isEqualTo(InvestmentTransactionType.SELL);

        assertThat(response.amount())
                .isEqualByComparingTo("1600.00");

        // Selling 5 of the 10 owned units leaves 5.
        assertThat(investment.getQuantity())
                .isEqualByComparingTo("5");

        verify(investmentTransactionRepository)
                .save(any(InvestmentTransaction.class));

        verify(investmentRepository)
                .save(investment);
    }

    @Test
    void sell_shouldRejectWhenSellingMoreThanOwnedQuantity() {

        investment.setQuantity(new BigDecimal("5"));
        investment.setAveragePurchasePrice(new BigDecimal("300.00"));

        SellInvestmentRequest request = new SellInvestmentRequest(
                account.getId(),
                new BigDecimal("1800.00"),
                new BigDecimal("6"),
                new BigDecimal("300.00"),
                OffsetDateTime.now()
        );

        // Account access is user-scoped to enforce account ownership.
        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        assertThatThrownBy(() ->
                investmentService.sell(
                        user.getId(),
                        investment.getId(),
                        request
                )
        )
        .isInstanceOf(IllegalArgumentException.class);

        verify(investmentTransactionRepository, never())
                .save(any());

        verify(investmentRepository, never())
                .save(any());
    }

    @Test
    void deleteInvestment_shouldRejectWhenTransactionHistoryExists() {

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(investmentTransactionRepository
                .existsByInvestment_Id(investment.getId()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                investmentService.deleteInvestment(
                        user.getId(),
                        investment.getId()
                )
        )
        .isInstanceOf(IllegalArgumentException.class);

        verify(investmentRepository, never())
                .delete(any(Investment.class));
    }

    @Test
    void deleteInvestment_shouldDeleteWhenNoTransactionHistoryExists() {

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(investmentTransactionRepository
                .existsByInvestment_Id(investment.getId()))
                .thenReturn(false);

        investmentService.deleteInvestment(
                user.getId(),
                investment.getId()
        );

        verify(investmentRepository)
                .delete(investment);
    }

    @Test
    void getTransactions_shouldReturnInvestmentHistory() {

        InvestmentTransaction transaction = new InvestmentTransaction(
                user,
                investment,
                account,
                InvestmentTransactionType.BUY,
                new BigDecimal("3000.00"),
                new BigDecimal("10"),
                new BigDecimal("300.00"),
                OffsetDateTime.now()
        );

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(investmentTransactionRepository
                .findByInvestment_Id(investment.getId()))
                .thenReturn(List.of(transaction));

        var response = investmentService.getTransactions(
                user.getId(),
                investment.getId()
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).type())
                .isEqualTo(InvestmentTransactionType.BUY);
        assertThat(response.get(0).amount())
                .isEqualByComparingTo("3000.00");
    }

    @Test
    void getPortfolio_shouldCalculateInvestedValueAndGainLoss() {

        Investment first = new Investment(
                user,
                "Nifty 50 ETF",
                InvestmentType.ETF
        );

        Investment second = new Investment(
                user,
                "Gold",
                InvestmentType.GOLD
        );

        first.setQuantity(new BigDecimal("10"));
        first.setAveragePurchasePrice(new BigDecimal("300.00"));
        first.setCurrentValue(new BigDecimal("3500.00"));

        second.setQuantity(new BigDecimal("5"));
        second.setAveragePurchasePrice(new BigDecimal("500.00"));
        second.setCurrentValue(new BigDecimal("3000.00"));

        InvestmentTransaction firstBuy = new InvestmentTransaction(
                user,
                first,
                account,
                InvestmentTransactionType.BUY,
                new BigDecimal("3000.00"),
                new BigDecimal("10"),
                new BigDecimal("300.00"),
                OffsetDateTime.now()
        );

        InvestmentTransaction secondBuy = new InvestmentTransaction(
                user,
                second,
                account,
                InvestmentTransactionType.BUY,
                new BigDecimal("2500.00"),
                new BigDecimal("5"),
                new BigDecimal("500.00"),
                OffsetDateTime.now()
        );

        when(investmentRepository.findByUser_Id(user.getId()))
                .thenReturn(List.of(first, second));

        // Portfolio totals are calculated from each investment's transaction history.
        when(investmentTransactionRepository
                .findByInvestment_Id(first.getId()))
                .thenReturn(List.of(firstBuy));

        when(investmentTransactionRepository
                .findByInvestment_Id(second.getId()))
                .thenReturn(List.of(secondBuy));

        var response = investmentService.getPortfolio(user.getId());

        assertThat(response.totalInvested())
                .isEqualByComparingTo("5500.00");

        assertThat(response.currentValue())
                .isEqualByComparingTo("6500.00");

        assertThat(response.gainLoss())
                .isEqualByComparingTo("1000.00");

        assertThat(response.returnPercentage())
                .isEqualByComparingTo("18.18");

        assertThat(response.investments())
                .hasSize(2);
    }
}
