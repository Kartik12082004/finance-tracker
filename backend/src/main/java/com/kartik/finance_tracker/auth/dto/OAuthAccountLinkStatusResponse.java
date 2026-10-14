package com.kartik.finance_tracker.auth.dto;

public record OAuthAccountLinkStatusResponse(
        boolean googleLinked,
        boolean githubLinked
) {
}
