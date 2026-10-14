package com.kartik.finance_tracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.kartik.finance_tracker.auth.dto.AuthResponse;
import com.kartik.finance_tracker.auth.dto.LoginRequest;
import com.kartik.finance_tracker.auth.dto.OAuthHandoffRequest;
import com.kartik.finance_tracker.auth.dto.RefreshTokenRequest;
import com.kartik.finance_tracker.auth.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthHandoffService oauthHandoffService;

    public AuthController(AuthService authService, OAuthHandoffService oauthHandoffService) {
        this.authService = authService;
        this.oauthHandoffService = oauthHandoffService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/oauth/exchange")
    public AuthResponse exchangeOAuthHandoff(
            @Valid @RequestBody OAuthHandoffRequest request,
            HttpServletRequest httpRequest
    ) {
        var session = httpRequest.getSession(false);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OAuth browser session is missing or expired"
            );
        }

        return oauthHandoffService.exchangeCode(
                request.code(),
                session.getId()
        );
    }

}
