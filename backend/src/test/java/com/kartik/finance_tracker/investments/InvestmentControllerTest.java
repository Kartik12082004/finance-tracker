package com.kartik.finance_tracker.investments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kartik.finance_tracker.investments.dto.InvestmentResponse;
import com.kartik.finance_tracker.investments.dto.InvestmentTransactionResponse;
import com.kartik.finance_tracker.investments.dto.PortfolioResponse;
import com.kartik.finance_tracker.security.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class InvestmentControllerTest {

    private InvestmentService investmentService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    private UUID userId;
    private UUID investmentId;
    private UUID accountId;

    @BeforeEach
    void setUp() {

        investmentService = mock(InvestmentService.class);
        currentUserService = mock(CurrentUserService.class);

        InvestmentController controller =
                new InvestmentController(
                        investmentService,
                        currentUserService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        userId = UUID.randomUUID();
        investmentId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void shouldCreateInvestment() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentResponse response =
                createInvestmentResponse();

        when(investmentService.createInvestment(
                eq(userId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "name": "NVIDIA",
                    "type": "STOCK"
                }
                """;

        mockMvc.perform(
                post("/api/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id")
                .value(investmentId.toString()))
        .andExpect(jsonPath("$.name")
                .value("NVIDIA"))
        .andExpect(jsonPath("$.type")
                .value("STOCK"))
        .andExpect(jsonPath("$.investedAmount")
                .value(10000.00))
        .andExpect(jsonPath("$.currentValue")
                .value(12000.00));

        verify(investmentService)
                .createInvestment(eq(userId), any());
    }

    @Test
    void shouldGetInvestments() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentResponse response =
                createInvestmentResponse();

        when(investmentService.getInvestments(userId))
                .thenReturn(List.of(response));

        mockMvc.perform(
                get("/api/investments")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()")
                .value(1))
        .andExpect(jsonPath("$[0].name")
                .value("NVIDIA"))
        .andExpect(jsonPath("$[0].type")
                .value("STOCK"))
        .andExpect(jsonPath("$[0].investedAmount")
                .value(10000.00));

        verify(investmentService)
                .getInvestments(userId);
    }

    @Test
    void shouldGetPortfolio() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentResponse investment =
                createInvestmentResponse();

        PortfolioResponse response =
                new PortfolioResponse(
                        new BigDecimal("10000.00"),
                        new BigDecimal("12000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("20.00"),
                        List.of(investment)
                );

        when(investmentService.getPortfolio(userId))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/investments/portfolio")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalInvested")
                .value(10000.00))
        .andExpect(jsonPath("$.currentValue")
                .value(12000.00))
        .andExpect(jsonPath("$.gainLoss")
                .value(2000.00))
        .andExpect(jsonPath("$.returnPercentage")
                .value(20.00))
        .andExpect(jsonPath("$.investments.length()")
                .value(1))
        .andExpect(jsonPath("$.investments[0].investedAmount")
                .value(10000.00));

        verify(investmentService)
                .getPortfolio(userId);
    }

    @Test
    void shouldGetInvestment() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentResponse response =
                createInvestmentResponse();

        when(investmentService.getInvestment(
                userId,
                investmentId
        )).thenReturn(response);

        mockMvc.perform(
                get(
                        "/api/investments/{investmentId}",
                        investmentId
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
                .value(investmentId.toString()))
        .andExpect(jsonPath("$.name")
                .value("NVIDIA"))
        .andExpect(jsonPath("$.investedAmount")
                .value(10000.00));

        verify(investmentService)
                .getInvestment(
                        userId,
                        investmentId
                );
    }

    @Test
    void shouldUpdateCurrentValueOnly() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentResponse response =
                createInvestmentResponse();

        when(investmentService.updateInvestment(
                eq(userId),
                eq(investmentId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "currentValue": 12000.00
                }
                """;

        mockMvc.perform(
                put(
                        "/api/investments/{investmentId}",
                        investmentId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentValue")
                .value(12000.00));

        verify(investmentService)
                .updateInvestment(
                        eq(userId),
                        eq(investmentId),
                        any()
                );
    }

    @Test
    void shouldDeleteInvestment() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(
                delete(
                        "/api/investments/{investmentId}",
                        investmentId
                )
        )
        .andExpect(status().isNoContent());

        verify(investmentService)
                .deleteInvestment(
                        userId,
                        investmentId
                );
    }

    @Test
    void shouldBuyInvestment() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentTransactionResponse response =
                createBuyResponse();

        when(investmentService.buy(
                eq(userId),
                eq(investmentId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "accountId": "%s",
                    "amount": 10000.00,
                    "quantity": 20.00,
                    "pricePerUnit": 500.00,
                    "occurredAt": "2026-09-23T10:00:00Z"
                }
                """.formatted(accountId);

        mockMvc.perform(
                post(
                        "/api/investments/{investmentId}/buys",
                        investmentId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.investmentId")
                .value(investmentId.toString()))
        .andExpect(jsonPath("$.accountId")
                .value(accountId.toString()))
        .andExpect(jsonPath("$.type")
                .value("BUY"))
        .andExpect(jsonPath("$.amount")
                .value(10000.00))
        .andExpect(jsonPath("$.quantity")
                .value(20.00))
        .andExpect(jsonPath("$.pricePerUnit")
                .value(500.00));

        verify(investmentService)
                .buy(
                        eq(userId),
                        eq(investmentId),
                        any()
                );
    }

    @Test
    void shouldSellInvestmentWithQuantityOnly() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentTransactionResponse response =
                createSellResponse();

        when(investmentService.sell(
                eq(userId),
                eq(investmentId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "accountId": "%s",
                    "quantity": 5.00,
                    "occurredAt": "2026-09-23T10:00:00Z"
                }
                """.formatted(accountId);

        mockMvc.perform(
                post(
                        "/api/investments/{investmentId}/sells",
                        investmentId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.investmentId")
                .value(investmentId.toString()))
        .andExpect(jsonPath("$.accountId")
                .value(accountId.toString()))
        .andExpect(jsonPath("$.type")
                .value("SELL"))
        .andExpect(jsonPath("$.quantity")
                .value(5.00))
        .andExpect(jsonPath("$.pricePerUnit")
                .value(600.00))
        .andExpect(jsonPath("$.amount")
                .value(3000.00));

        verify(investmentService)
                .sell(
                        eq(userId),
                        eq(investmentId),
                        any()
                );
    }

    @Test
    void shouldGetInvestmentTransactions() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        InvestmentTransactionResponse response =
                createBuyResponse();

        when(investmentService.getTransactions(
                userId,
                investmentId
        )).thenReturn(List.of(response));

        mockMvc.perform(
                get(
                        "/api/investments/{investmentId}/transactions",
                        investmentId
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()")
                .value(1))
        .andExpect(jsonPath("$[0].type")
                .value("BUY"))
        .andExpect(jsonPath("$[0].amount")
                .value(10000.00));

        verify(investmentService)
                .getTransactions(
                        userId,
                        investmentId
                );
    }

    @Test
    void shouldRejectMissingBuyAccount() throws Exception {

        String request = """
                {
                    "amount": 10000.00,
                    "quantity": 20.00,
                    "pricePerUnit": 500.00,
                    "occurredAt": "2026-09-23T10:00:00Z"
                }
                """;

        mockMvc.perform(
                post(
                        "/api/investments/{investmentId}/buys",
                        investmentId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingBuyAmount() throws Exception {

        String request = """
                {
                    "accountId": "%s",
                    "quantity": 20.00,
                    "pricePerUnit": 500.00,
                    "occurredAt": "2026-09-23T10:00:00Z"
                }
                """.formatted(accountId);

        mockMvc.perform(
                post(
                        "/api/investments/{investmentId}/buys",
                        investmentId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingSellQuantity() throws Exception {

        String request = """
                {
                    "accountId": "%s",
                    "occurredAt": "2026-09-23T10:00:00Z"
                }
                """.formatted(accountId);

        mockMvc.perform(
                post(
                        "/api/investments/{investmentId}/sells",
                        investmentId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativeCurrentValue() throws Exception {

        String request = """
                {
                    "currentValue": -100.00
                }
                """;

        mockMvc.perform(
                put(
                        "/api/investments/{investmentId}",
                        investmentId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    private InvestmentResponse createInvestmentResponse() {

        return new InvestmentResponse(
                investmentId,
                "NVIDIA",
                InvestmentType.STOCK,
                new BigDecimal("20.00"),
                new BigDecimal("500.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("12000.00"),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private InvestmentTransactionResponse createBuyResponse() {

        return new InvestmentTransactionResponse(
                UUID.randomUUID(),
                investmentId,
                accountId,
                InvestmentTransactionType.BUY,
                new BigDecimal("10000.00"),
                new BigDecimal("20.00"),
                new BigDecimal("500.00"),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private InvestmentTransactionResponse createSellResponse() {

        return new InvestmentTransactionResponse(
                UUID.randomUUID(),
                investmentId,
                accountId,
                InvestmentTransactionType.SELL,
                new BigDecimal("3000.00"),
                new BigDecimal("5.00"),
                new BigDecimal("600.00"),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
