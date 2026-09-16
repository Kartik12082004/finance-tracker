package com.kartik.finance_tracker.accounts;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kartik.finance_tracker.AbstractPostgresIntegrationTest;
import com.kartik.finance_tracker.auth.jwt.JwtService;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@SpringBootTest
@Transactional
class AccountSecurityIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User(
                "security-test@example.com",
                "hashed-password",
                "Security Test User"
        );

        userRepository.save(user);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        // Account endpoints require authentication.
        // A request without a JWT must therefore be rejected.
        mockMvc.perform(
                get("/api/accounts")
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedRequestWithValidJwt() throws Exception {

        // The JWT subject identifies the Finance Tracker user.
        String token = jwtService.generateAccessToken(user.getId());

        // Spring Security must validate the real JWT before the request
        // reaches the controller.
        mockMvc.perform(
                get("/api/accounts")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidJwt() throws Exception {

        // A malformed/invalid bearer token must never reach the controller.
        mockMvc.perform(
                get("/api/accounts")
                        .header(
                                "Authorization",
                                "Bearer invalid-token"
                        )
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAccessToAnotherUsersAccount() throws Exception {

        // User A must not be able to access an account owned by User B.
        User otherUser = new User(
                "other-user@example.com",
                "hashed-password",
                "Other User"
        );
        userRepository.save(otherUser);

        Account otherAccount = new Account(
                otherUser,
                "Other User Account",
                AccountType.BANK,
                "INR"
        );
        accountRepository.save(otherAccount);

        String accessToken = jwtService.generateAccessToken(user.getId());

        mockMvc.perform(
                get("/api/accounts/{accountId}", otherAccount.getId())
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
        )
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class MockMvcConfig {

        @Bean
        MockMvc mockMvc(
                WebApplicationContext webApplicationContext
        ) {
            return MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply(springSecurity())
                    .build();
        }
    }
}
