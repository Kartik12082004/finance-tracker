package com.kartik.finance_tracker.auth;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.kartik.finance_tracker.auth.dto.OAuthAccountLinkStatusResponse;
import com.kartik.finance_tracker.security.CurrentUserService;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/account-links")
public class OAuthAccountLinkController {

    public static final String LINK_USER_ID_SESSION_KEY =
            "oauth_link_user_id";

    public static final String LINK_PROVIDER_SESSION_KEY =
            "oauth_link_provider";

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final OAuthAccountService oauthAccountService;

    public OAuthAccountLinkController(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository,
            OAuthAccountService oauthAccountService
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.oauthAccountService = oauthAccountService;
    }

    @GetMapping
    public ResponseEntity<OAuthAccountLinkStatusResponse> getAccountLinkStatus() {
        UUID userId = currentUserService.getCurrentUserId();

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user could not be found"
            );
        }

        List<OAuthAccount> linkedAccounts =
                oauthAccountRepository.findAllByUserId(userId);

        boolean googleLinked = linkedAccounts.stream()
                .anyMatch(account ->
                        account.getProvider() == OAuthProvider.GOOGLE);

        boolean githubLinked = linkedAccounts.stream()
                .anyMatch(account ->
                        account.getProvider() == OAuthProvider.GITHUB);

        return ResponseEntity.ok(
                new OAuthAccountLinkStatusResponse(
                        googleLinked,
                        githubLinked
                )
        );
    }

    @PostMapping("/github")
    public ResponseEntity<Void> initiateGitHubLink(
            HttpServletRequest request
    ) {
        return initiateLink(request, OAuthProvider.GITHUB);
    }

    @PostMapping("/google")
    public ResponseEntity<Void> initiateGoogleLink(
            HttpServletRequest request
    ) {
        return initiateLink(request, OAuthProvider.GOOGLE);
    }

    @DeleteMapping("/github")
    public ResponseEntity<Void> unlinkGitHubAccount() {
        return unlinkAccount(OAuthProvider.GITHUB);
    }

    @DeleteMapping("/google")
    public ResponseEntity<Void> unlinkGoogleAccount() {
        return unlinkAccount(OAuthProvider.GOOGLE);
    }

    private ResponseEntity<Void> initiateLink(
            HttpServletRequest request,
            OAuthProvider provider
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user could not be found"
            );
        }

        HttpSession session = request.getSession(true);

        session.setAttribute(
                LINK_USER_ID_SESSION_KEY,
                userId.toString()
        );

        session.setAttribute(
                LINK_PROVIDER_SESSION_KEY,
                provider.name()
        );

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> unlinkAccount(
            OAuthProvider provider
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user could not be found"
                ));

        try {
            oauthAccountService.unlinkOAuthAccount(user, provider);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    exception.getMessage()
            );
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    exception.getMessage()
            );
        }
    }
}
