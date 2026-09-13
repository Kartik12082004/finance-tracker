package com.kartik.finance_tracker.common.dto;

public record ApiErrorResponse(
        int status,
        String message
) {
}
