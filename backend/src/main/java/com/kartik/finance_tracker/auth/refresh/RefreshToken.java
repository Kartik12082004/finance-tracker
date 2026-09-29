package com.kartik.finance_tracker.auth.refresh;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.users.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    public RefreshToken(
            User user,
            String tokenHash,
            OffsetDateTime expiresAt
    ) {
        // Generate the database ID in the application.
        this.id = UUID.randomUUID();

        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;

        // Record when this refresh token was created.
        this.createdAt = OffsetDateTime.now();
    }

    @Id
    private UUID id;

    // Each refresh token belongs to one Finance Tracker user.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Only the SHA-256 hash of the refresh token is stored.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // NULL means the token has not been revoked.
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
}
