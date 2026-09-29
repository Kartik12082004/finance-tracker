package com.kartik.finance_tracker.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public UUID getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {

            throw new IllegalStateException(
                    "No authenticated user is available"
            );
        }

        // The JWT subject contains the authenticated Finance Tracker user ID.
        return UUID.fromString(jwt.getSubject());
    }
}
