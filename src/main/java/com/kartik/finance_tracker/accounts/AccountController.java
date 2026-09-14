package com.kartik.finance_tracker.accounts;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.accounts.dto.AccountResponse;
import com.kartik.finance_tracker.accounts.dto.CreateAccountRequest;
import com.kartik.finance_tracker.security.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserService currentUserService;

    public AccountController(
            AccountService accountService,
            CurrentUserService currentUserService
    ) {
        this.accountService = accountService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {

        // The authenticated user's ID comes from Spring Security,
        // rather than from a client-supplied header or request body.
        var userId = currentUserService.getCurrentUserId();

        Account account = accountService.createAccount(
                userId,
                request.name(),
                request.type(),
                request.currency()
        );

        // Convert the domain entity into a response DTO
        // so internal entity details are not exposed directly.
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getOpeningBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    @GetMapping
    public List<AccountResponse> getAccounts() {

        // The authenticated user's ID comes from Spring Security.
        UUID userId = currentUserService.getCurrentUserId();

        return accountService.getAccounts(userId)
                .stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getName(),
                        account.getType(),
                        account.getCurrency(),
                        account.getOpeningBalance(),
                        account.getCreatedAt(),
                        account.getUpdatedAt()
                ))
                .toList();
    }
}
