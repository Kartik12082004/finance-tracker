package com.kartik.finance_tracker.accounts;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.accounts.dto.AccountResponse;
import com.kartik.finance_tracker.accounts.dto.CreateAccountRequest;
import com.kartik.finance_tracker.accounts.dto.UpdateAccountRequest;
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
        UUID userId = currentUserService.getCurrentUserId();

        Account account = accountService.createAccount(
                userId,
                request.name(),
                request.type(),
                request.currency(),
                request.openingBalance(),
                request.creditLimit(),
                request.parentAccountId()
        );

        // The controller converts the domain entity into a response DTO
        // so internal entity details are not exposed directly through the API.
        return toResponse(account);
    }

    @PutMapping("/{accountId}")
    public AccountResponse updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        // The authenticated user's ID comes from the JWT.
        // The client cannot choose which user's account to update.
        UUID userId = currentUserService.getCurrentUserId();

        Account account = accountService.updateAccount(
                userId,
                accountId,
                request.name(),
                request.type(),
                request.currency(),
                request.creditLimit(),
                request.parentAccountId()
        );

        return toResponse(account);
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @PathVariable UUID accountId
    ) {
        // The authenticated user's ID comes from the JWT.
        UUID userId = currentUserService.getCurrentUserId();

        accountService.deleteAccount(userId, accountId);
    }

    @GetMapping
    public List<AccountResponse> getAccounts() {

        // The authenticated user's ID comes from Spring Security.
        UUID userId = currentUserService.getCurrentUserId();

        return accountService.getAccounts(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(
            @PathVariable UUID accountId
    ) {
        // Never trust the client to tell us which user owns the account.
        // The authenticated user's ID comes from the JWT.
        UUID userId = currentUserService.getCurrentUserId();

        Account account = accountService.getAccount(
                userId,
                accountId
        );

        return toResponse(account);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getOpeningBalance(),
                account.getCreditLimit(),
                account.getParentAccount() == null
                        ? null
                        : account.getParentAccount().getId(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
