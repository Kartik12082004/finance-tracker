package com.kartik.finance_tracker.auth.refresh;

import com.kartik.finance_tracker.users.User;

public record RefreshTokenRotation(
    User user,
    String refreshToken
) {
}
