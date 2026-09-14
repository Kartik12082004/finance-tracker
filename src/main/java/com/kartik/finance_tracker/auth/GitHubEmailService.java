package com.kartik.finance_tracker.auth;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubEmailService {

    private final RestClient restClient;

    public GitHubEmailService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2026-03-10")
                .build();
    }

    public String findVerifiedEmail(String accessToken) {

        List<GitHubEmail> emails = restClient.get()
                .uri("/user/emails")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<GitHubEmail>>() {});

        if (emails == null || emails.isEmpty()) {
            return null;
        }

        // Prefer a verified primary email, then any other verified email.
        return emails.stream()
                .filter(GitHubEmail::verified)
                .sorted(Comparator.comparing(email -> !email.primary()))
                .map(GitHubEmail::email)
                .filter(email -> email != null && !email.isBlank())
                .findFirst()
                .orElse(null);
    }

    private record GitHubEmail(
            String email,
            boolean primary,
            boolean verified
    ) {
    }
}