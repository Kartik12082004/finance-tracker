package com.kartik.finance_tracker.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthHandoffRequest(

        @NotBlank(message = "OAuth handoff code is required")
        String code
) {
}
