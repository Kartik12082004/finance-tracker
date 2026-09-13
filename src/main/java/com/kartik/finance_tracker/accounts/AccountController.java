package com.kartik.finance_tracker.accounts;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.accounts.dto.AccountResponse;
import com.kartik.finance_tracker.accounts.dto.CreateAccountRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateAccountRequest request
    ) {
        // X-User-Id is temporary as of right now, since we don't have authentication implemented yet.
        // Once authentication is implemented, the user ID will come from the authenticated JWT.
        Account account = accountService.createAccount(
                userId,
                request.name(),
                request.type(),
                request.currency()
        );

        // The controller converts the domain entity into a response DTO
        // so internal entity details are not exposed directly through the API.
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
    public List<AccountResponse> getAccounts(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        // X-User-Id is temporary as of right now, since we don't have authentication implemented yet.
        // Once authentication is implemented, the user ID will come from the authenticated JWT.
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