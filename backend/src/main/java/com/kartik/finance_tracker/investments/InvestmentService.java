package com.kartik.finance_tracker.investments;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.investments.dto.BuyInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.CreateInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.InvestmentResponse;
import com.kartik.finance_tracker.investments.dto.InvestmentTransactionResponse;
import com.kartik.finance_tracker.investments.dto.PortfolioResponse;
import com.kartik.finance_tracker.investments.dto.SellInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.UpdateInvestmentRequest;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final InvestmentTransactionRepository investmentTransactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;

    public InvestmentService(
            InvestmentRepository investmentRepository,
            InvestmentTransactionRepository investmentTransactionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountBalanceService accountBalanceService
    ) {
        this.investmentRepository = investmentRepository;
        this.investmentTransactionRepository =
                investmentTransactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
    }

    @Transactional
    public InvestmentResponse createInvestment(
            UUID userId,
            CreateInvestmentRequest request
    ) {
        User user = getUser(userId);

        Investment investment = new Investment(
                user,
                request.name(),
                request.type()
        );

        return toResponse(investmentRepository.save(investment));
    }

    @Transactional(readOnly = true)
    public List<InvestmentResponse> getInvestments(UUID userId) {
        getUser(userId);

        return investmentRepository.findByUser_Id(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvestmentResponse getInvestment(
            UUID userId,
            UUID investmentId
    ) {
        return toResponse(getInvestmentEntity(userId, investmentId));
    }

    @Transactional
    public InvestmentResponse updateInvestment(
            UUID userId,
            UUID investmentId,
            UpdateInvestmentRequest request
    ) {
        Investment investment =
                getInvestmentEntity(userId, investmentId);

        investment.update(
                request.name(),
                request.type(),
                request.currentValue()
        );

        return toResponse(investmentRepository.save(investment));
    }

    @Transactional
    public void deleteInvestment(
            UUID userId,
            UUID investmentId
    ) {
        Investment investment =
                getInvestmentEntity(userId, investmentId);

        /*
         * Investment history should not disappear accidentally.
         * Once an investment has BUY/SELL records, keep the history.
         */
        if (investmentTransactionRepository
                .existsByInvestment_Id(investmentId)) {
            throw new IllegalArgumentException(
                    "Investment with transaction history cannot be deleted"
            );
        }

        investmentRepository.delete(investment);
    }

    @Transactional
    public InvestmentTransactionResponse buy(
            UUID userId,
            UUID investmentId,
            BuyInvestmentRequest request
    ) {
        Investment investment =
                getInvestmentEntity(userId, investmentId);

        Account account =
                getAccountForUser(userId, request.accountId());

        validateAmount(request.amount());

        /*
         * BUY moves money out of the selected account.
         * The account balance service includes investment transactions,
         * so the balance check reflects previous investment purchases.
         */
        BigDecimal currentBalance =
                accountBalanceService.calculateBalance(account.getId());

        if (request.amount().compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        validateQuantityAndPrice(
                request.quantity(),
                request.pricePerUnit(),
                request.amount()
        );

        InvestmentTransaction transaction =
                new InvestmentTransaction(
                        account.getUser(),
                        investment,
                        account,
                        InvestmentTransactionType.BUY,
                        request.amount(),
                        request.quantity(),
                        request.pricePerUnit(),
                        request.occurredAt()
                );

        updateInvestmentAfterBuy(
                investment,
                request.amount(),
                request.quantity()
        );

        InvestmentTransaction savedTransaction =
                investmentTransactionRepository.save(transaction);

        investmentRepository.save(investment);

        return toTransactionResponse(savedTransaction);
    }

    @Transactional
    public InvestmentTransactionResponse sell(
            UUID userId,
            UUID investmentId,
            SellInvestmentRequest request
    ) {
        Investment investment =
                getInvestmentEntity(userId, investmentId);

        Account account =
                getAccountForUser(userId, request.accountId());

        validateAmount(request.amount());

        if (request.quantity() != null) {
            if (investment.getQuantity() == null) {
                throw new IllegalArgumentException(
                        "Investment does not have tracked quantity"
                );
            }

            if (request.quantity()
                    .compareTo(investment.getQuantity()) > 0) {
                throw new IllegalArgumentException(
                        "Cannot sell more units than currently owned"
                );
            }
        }

        validateQuantityAndPrice(
                request.quantity(),
                request.pricePerUnit(),
                request.amount()
        );

        InvestmentTransaction transaction =
                new InvestmentTransaction(
                        account.getUser(),
                        investment,
                        account,
                        InvestmentTransactionType.SELL,
                        request.amount(),
                        request.quantity(),
                        request.pricePerUnit(),
                        request.occurredAt()
                );

        updateInvestmentAfterSell(
                investment,
                request.amount(),
                request.quantity()
        );

        InvestmentTransaction savedTransaction =
                investmentTransactionRepository.save(transaction);

        investmentRepository.save(investment);

        return toTransactionResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<InvestmentTransactionResponse> getTransactions(
            UUID userId,
            UUID investmentId
    ) {
        getInvestmentEntity(userId, investmentId);

        return investmentTransactionRepository
                .findByInvestment_Id(investmentId)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(UUID userId) {
        List<Investment> investments =
                investmentRepository.findByUser_Id(userId);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;

        for (Investment investment : investments) {
            List<InvestmentTransaction> transactions =
                    investmentTransactionRepository
                            .findByInvestment_Id(investment.getId());

            for (InvestmentTransaction transaction : transactions) {
                if (transaction.getType()
                        == InvestmentTransactionType.BUY) {
                    totalInvested = totalInvested.add(
                            transaction.getAmount()
                    );
                } else {
                    totalInvested = totalInvested.subtract(
                            transaction.getAmount()
                    );
                }
            }

            currentValue = currentValue.add(
                    investment.getCurrentValue()
            );
        }

        BigDecimal gainLoss =
                currentValue.subtract(totalInvested);

        BigDecimal returnPercentage = BigDecimal.ZERO;

        if (totalInvested.signum() > 0) {
            returnPercentage = gainLoss
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            totalInvested,
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        return new PortfolioResponse(
                totalInvested,
                currentValue,
                gainLoss,
                returnPercentage,
                investments.stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private void updateInvestmentAfterBuy(
            Investment investment,
            BigDecimal amount,
            BigDecimal quantity
    ) {
        if (quantity == null) {
            return;
        }

        BigDecimal oldQuantity =
                investment.getQuantity() == null
                        ? BigDecimal.ZERO
                        : investment.getQuantity();

        BigDecimal oldAveragePrice =
                investment.getAveragePurchasePrice() == null
                        ? BigDecimal.ZERO
                        : investment.getAveragePurchasePrice();

        BigDecimal oldCost =
                oldQuantity.multiply(oldAveragePrice);

        BigDecimal newQuantity =
                oldQuantity.add(quantity);

        BigDecimal newAveragePrice =
                oldCost.add(amount)
                        .divide(
                                newQuantity,
                                4,
                                RoundingMode.HALF_UP
                        );

        investment.setQuantity(newQuantity);
        investment.setAveragePurchasePrice(newAveragePrice);
    }

    private void updateInvestmentAfterSell(
            Investment investment,
            BigDecimal amount,
            BigDecimal quantity
    ) {
        if (quantity == null || investment.getQuantity() == null) {
            return;
        }

        BigDecimal remainingQuantity =
                investment.getQuantity().subtract(quantity);

        if (remainingQuantity.signum() == 0) {
            investment.setQuantity(BigDecimal.ZERO);
            return;
        }

        investment.setQuantity(remainingQuantity);
    }

    private void validateQuantityAndPrice(
            BigDecimal quantity,
            BigDecimal pricePerUnit,
            BigDecimal amount
    ) {
        /*
         * Quantity and price are optional in V1.
         * If both are supplied, make sure they agree with the amount.
         */
        if (quantity != null && pricePerUnit != null) {
            BigDecimal calculatedAmount =
                    quantity.multiply(pricePerUnit);

            if (calculatedAmount.compareTo(amount) != 0) {
                throw new IllegalArgumentException(
                        "Quantity multiplied by price per unit must equal amount"
                );
            }
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

        if (amount.scale() > 4) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 4 decimal places"
            );
        }
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private Account getAccountForUser(
            UUID userId,
            UUID accountId
    ) {
        return accountRepository.findByIdAndUser_Id(
                accountId,
                userId
        ).orElseThrow(() ->
                new ResourceNotFoundException("Account not found"));
    }

    private Investment getInvestmentEntity(
            UUID userId,
            UUID investmentId
    ) {
        return investmentRepository
                .findByIdAndUser_Id(investmentId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Investment not found"
                        ));
    }

    private InvestmentResponse toResponse(
            Investment investment
    ) {
        return new InvestmentResponse(
                investment.getId(),
                investment.getName(),
                investment.getType(),
                investment.getQuantity(),
                investment.getAveragePurchasePrice(),
                investment.getCurrentValue(),
                investment.getCreatedAt(),
                investment.getUpdatedAt()
        );
    }

    private InvestmentTransactionResponse toTransactionResponse(
            InvestmentTransaction transaction
    ) {
        return new InvestmentTransactionResponse(
                transaction.getId(),
                transaction.getInvestment().getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getOccurredAt(),
                transaction.getCreatedAt()
        );
    }
}
