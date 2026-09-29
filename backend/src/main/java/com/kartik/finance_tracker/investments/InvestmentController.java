package com.kartik.finance_tracker.investments;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.investments.dto.BuyInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.CreateInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.InvestmentResponse;
import com.kartik.finance_tracker.investments.dto.InvestmentTransactionResponse;
import com.kartik.finance_tracker.investments.dto.PortfolioResponse;
import com.kartik.finance_tracker.investments.dto.SellInvestmentRequest;
import com.kartik.finance_tracker.investments.dto.UpdateInvestmentRequest;
import com.kartik.finance_tracker.security.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final CurrentUserService currentUserService;

    public InvestmentController(
            InvestmentService investmentService,
            CurrentUserService currentUserService
    ) {
        this.investmentService = investmentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<InvestmentResponse> createInvestment(
            @Valid @RequestBody CreateInvestmentRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(investmentService.createInvestment(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<InvestmentResponse>> getInvestments() {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                investmentService.getInvestments(userId)
        );
    }

    /*
     * Keep the portfolio route before /{investmentId}.
     * "portfolio" is a fixed endpoint, not an investment UUID.
     */
    @GetMapping("/portfolio")
    public ResponseEntity<PortfolioResponse> getPortfolio() {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                investmentService.getPortfolio(userId)
        );
    }

    @GetMapping("/{investmentId}")
    public ResponseEntity<InvestmentResponse> getInvestment(
            @PathVariable UUID investmentId
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                investmentService.getInvestment(userId, investmentId)
        );
    }

    @PutMapping("/{investmentId}")
    public ResponseEntity<InvestmentResponse> updateInvestment(
            @PathVariable UUID investmentId,
            @Valid @RequestBody UpdateInvestmentRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                investmentService.updateInvestment(
                        userId,
                        investmentId,
                        request
                )
        );
    }

    @DeleteMapping("/{investmentId}")
    public ResponseEntity<Void> deleteInvestment(
            @PathVariable UUID investmentId
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        investmentService.deleteInvestment(userId, investmentId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{investmentId}/buys")
    public ResponseEntity<InvestmentTransactionResponse> buy(
            @PathVariable UUID investmentId,
            @Valid @RequestBody BuyInvestmentRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        investmentService.buy(
                                userId,
                                investmentId,
                                request
                        )
                );
    }

    @PostMapping("/{investmentId}/sells")
    public ResponseEntity<InvestmentTransactionResponse> sell(
            @PathVariable UUID investmentId,
            @Valid @RequestBody SellInvestmentRequest request
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        investmentService.sell(
                                userId,
                                investmentId,
                                request
                        )
                );
    }

    @GetMapping("/{investmentId}/transactions")
    public ResponseEntity<List<InvestmentTransactionResponse>> getTransactions(
            @PathVariable UUID investmentId
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                investmentService.getTransactions(
                        userId,
                        investmentId
                )
        );
    }
}
