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
        investmentTransactionRepository =
                mock(InvestmentTransactionRepository.class);
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
                "INR",
                new BigDecimal("100000.00")
        );

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

        CreateInvestmentRequest request =
                new CreateInvestmentRequest(
                        "Nifty 50 ETF",
                        InvestmentType.ETF
                );

        when(investmentRepository.save(any(Investment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.createInvestment(
                user.getId(),
                request
        );

        assertThat(response.name())
                .isEqualTo("Nifty 50 ETF");

        assertThat(response.type())
                .isEqualTo(InvestmentType.ETF);

        assertThat(response.currentValue())
                .isEqualByComparingTo("0");

        assertThat(response.investedAmount())
                .isEqualByComparingTo("0");

        verify(investmentRepository)
                .save(any(Investment.class));
    }

    @Test
    void getInvestments_shouldReturnUsersInvestments() {

        when(investmentRepository.findByUser_Id(user.getId()))
                .thenReturn(List.of(investment));

        when(investmentTransactionRepository
                .findByInvestment_Id(investment.getId()))
                .thenReturn(List.of());

        var response =
                investmentService.getInvestments(user.getId());

        assertThat(response)
                .hasSize(1);

        assertThat(response.get(0).name())
                .isEqualTo("Nifty 50 ETF");

        assertThat(response.get(0).investedAmount())
                .isEqualByComparingTo("0");
    }

    @Test
    void updateInvestment_shouldUpdateCurrentValueOnly() {

        UpdateInvestmentRequest request =
                new UpdateInvestmentRequest(
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

        assertThat(response.name())
                .isEqualTo("Nifty 50 ETF");

        assertThat(response.type())
                .isEqualTo(InvestmentType.ETF);

        assertThat(response.currentValue())
                .isEqualByComparingTo("12500.00");

        verify(investmentRepository)
                .save(investment);
    }

    @Test
    void buy_shouldCreateInvestmentTransactionAndUpdateHolding() {

        BuyInvestmentRequest request =
                new BuyInvestmentRequest(
                        account.getId(),
                        new BigDecimal("10"),
                        new BigDecimal("300.00"),
                        new BigDecimal("3000.00"),
                        OffsetDateTime.now()
                );

        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(accountBalanceService.calculateBalance(
                account.getId()
        )).thenReturn(new BigDecimal("10000.00"));

        when(investmentTransactionRepository.save(
                any(InvestmentTransaction.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        when(investmentRepository.save(
                any(Investment.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.buy(
                user.getId(),
                investment.getId(),
                request
        );

        assertThat(response.type())
                .isEqualTo(InvestmentTransactionType.BUY);

        assertThat(response.amount())
                .isEqualByComparingTo("3000.00");

        assertThat(response.quantity())
                .isEqualByComparingTo("10");

        assertThat(response.pricePerUnit())
                .isEqualByComparingTo("300.00");

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

        BuyInvestmentRequest request =
                new BuyInvestmentRequest(
                        account.getId(),
                        new BigDecimal("50"),
                        new BigDecimal("300.00"),
                        new BigDecimal("15000.00"),
                        OffsetDateTime.now()
                );

        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(accountBalanceService.calculateBalance(
                account.getId()
        )).thenReturn(new BigDecimal("10000.00"));

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

        BuyInvestmentRequest request =
                new BuyInvestmentRequest(
                        account.getId(),
                        new BigDecimal("10"),
                        new BigDecimal("250.00"),
                        new BigDecimal("3000.00"),
                        OffsetDateTime.now()
                );

        when(accountRepository.findByIdAndUser_Id(
                account.getId(),
                user.getId()
        )).thenReturn(Optional.of(account));

        when(investmentRepository.findByIdAndUser_Id(
                investment.getId(),
                user.getId()
        )).thenReturn(Optional.of(investment));

        when(accountBalanceService.calculateBalance(
                account.getId()
        )).thenReturn(new BigDecimal("10000.00"));

        assertThatThrownBy(() ->
                investmentService.buy(
                        user.getId(),
                        investment.getId(),
                        request
                )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
                "Quantity multiplied by price per unit must equal amount"
        );

        verify(investmentTransactionRepository, never())
                .save(any());
    }

    @Test
    void sell_shouldCalculateProceedsFromCurrentValue() {

        investment.setQuantity(
                new BigDecimal("10")
        );

        investment.setAveragePurchasePrice(
                new BigDecimal("300.00")
        );

        investment.setCurrentValue(
                new BigDecimal("3500.00")
        );

        SellInvestmentRequest request =
                new SellInvestmentRequest(
                        account.getId(),
                        new BigDecimal("5"),
                        OffsetDateTime.now()
                );

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

        when(investmentRepository.save(
                any(Investment.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.sell(
                user.getId(),
                investment.getId(),
                request
        );

        // ₹3500 / 10 units = ₹350 per unit.
        // Selling 5 units = ₹1750.
        assertThat(response.type())
                .isEqualTo(InvestmentTransactionType.SELL);

        assertThat(response.quantity())
                .isEqualByComparingTo("5");

        assertThat(response.pricePerUnit())
                .isEqualByComparingTo("350.0000");

        assertThat(response.amount())
                .isEqualByComparingTo("1750.0000");

        assertThat(investment.getQuantity())
                .isEqualByComparingTo("5");

        verify(investmentTransactionRepository)
                .save(any(InvestmentTransaction.class));

        verify(investmentRepository)
                .save(investment);
    }

    @Test
    void sell_shouldAllowSellingAllOwnedUnits() {

        investment.setQuantity(
                new BigDecimal("10")
        );

        investment.setAveragePurchasePrice(
                new BigDecimal("300.00")
        );

        investment.setCurrentValue(
                new BigDecimal("3500.00")
        );

        SellInvestmentRequest request =
                new SellInvestmentRequest(
                        account.getId(),
                        new BigDecimal("10"),
                        OffsetDateTime.now()
                );

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

        when(investmentRepository.save(
                any(Investment.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        var response = investmentService.sell(
                user.getId(),
                investment.getId(),
                request
        );

        assertThat(response.type())
                .isEqualTo(InvestmentTransactionType.SELL);

        assertThat(response.amount())
                .isEqualByComparingTo("3500.0000");

        assertThat(investment.getQuantity())
                .isEqualByComparingTo("0");

        verify(investmentTransactionRepository)
                .save(any(InvestmentTransaction.class));

        verify(investmentRepository)
                .save(investment);
    }

    @Test
    void sell_shouldRejectWhenSellingMoreThanOwnedQuantity() {

        investment.setQuantity(
                new BigDecimal("5")
        );

        investment.setCurrentValue(
                new BigDecimal("1750.00")
        );

        SellInvestmentRequest request =
                new SellInvestmentRequest(
                        account.getId(),
                        new BigDecimal("6"),
                        OffsetDateTime.now()
                );

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
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
                "Cannot sell more units than currently owned"
        );

        verify(investmentTransactionRepository, never())
                .save(any());

        verify(investmentRepository, never())
                .save(any());
    }

    @Test
    void sell_shouldRejectWhenInvestmentHasNoUnits() {

        investment.setQuantity(
                BigDecimal.ZERO
        );

        investment.setCurrentValue(
                BigDecimal.ZERO
        );

        SellInvestmentRequest request =
                new SellInvestmentRequest(
                        account.getId(),
                        new BigDecimal("1"),
                        OffsetDateTime.now()
                );

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
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
                "Cannot sell more units than currently owned"
        );

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
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
                "Investment with transaction history cannot be deleted"
        );

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

        InvestmentTransaction transaction =
                new InvestmentTransaction(
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

        assertThat(response)
                .hasSize(1);

        assertThat(response.get(0).type())
                .isEqualTo(InvestmentTransactionType.BUY);

        assertThat(response.get(0).amount())
                .isEqualByComparingTo("3000.00");
    }

    @Test
    void getInvestments_shouldCalculateInvestedAmountFromHistory() {

        InvestmentTransaction buy =
                new InvestmentTransaction(
                        user,
                        investment,
                        account,
                        InvestmentTransactionType.BUY,
                        new BigDecimal("10000.00"),
                        new BigDecimal("20"),
                        new BigDecimal("500.00"),
                        OffsetDateTime.now()
                );

        InvestmentTransaction sell =
                new InvestmentTransaction(
                        user,
                        investment,
                        account,
                        InvestmentTransactionType.SELL,
                        new BigDecimal("3000.00"),
                        new BigDecimal("5"),
                        new BigDecimal("600.00"),
                        OffsetDateTime.now()
                );

        when(investmentRepository.findByUser_Id(user.getId()))
                .thenReturn(List.of(investment));

        when(investmentTransactionRepository
                .findByInvestment_Id(investment.getId()))
                .thenReturn(List.of(buy, sell));

        var response =
                investmentService.getInvestments(user.getId());

        assertThat(response)
                .hasSize(1);

        assertThat(response.get(0).investedAmount())
                .isEqualByComparingTo("7000.00");
    }

    @Test
    void getPortfolio_shouldCalculateInvestedValueAndGainLoss() {

        Investment first =
                new Investment(
                        user,
                        "Nifty 50 ETF",
                        InvestmentType.ETF
                );

        Investment second =
                new Investment(
                        user,
                        "Gold",
                        InvestmentType.GOLD
                );

        first.setQuantity(
                new BigDecimal("10")
        );

        first.setAveragePurchasePrice(
                new BigDecimal("300.00")
        );

        first.setCurrentValue(
                new BigDecimal("3500.00")
        );

        second.setQuantity(
                new BigDecimal("5")
        );

        second.setAveragePurchasePrice(
                new BigDecimal("500.00")
        );

        second.setCurrentValue(
                new BigDecimal("3000.00")
        );

        InvestmentTransaction firstBuy =
                new InvestmentTransaction(
                        user,
                        first,
                        account,
                        InvestmentTransactionType.BUY,
                        new BigDecimal("3000.00"),
                        new BigDecimal("10"),
                        new BigDecimal("300.00"),
                        OffsetDateTime.now()
                );

        InvestmentTransaction secondBuy =
                new InvestmentTransaction(
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

        when(investmentTransactionRepository
                .findByInvestment_Id(first.getId()))
                .thenReturn(List.of(firstBuy));

        when(investmentTransactionRepository
                .findByInvestment_Id(second.getId()))
                .thenReturn(List.of(secondBuy));

        var response =
                investmentService.getPortfolio(user.getId());

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

        assertThat(response.investments().get(0).investedAmount())
                .isEqualByComparingTo("3000.00");

        assertThat(response.investments().get(1).investedAmount())
                .isEqualByComparingTo("2500.00");
    }
}
