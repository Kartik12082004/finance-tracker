package com.kartik.finance_tracker.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.users.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_oauth_provider_user",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
public class OAuthAccount {

    public OAuthAccount(
            User user,
            OAuthProvider provider,
            String providerUserId
    ) {
        // Generate the OAuth account ID in the application.
        this.id = UUID.randomUUID();

        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;

        // Set both timestamps when the OAuth account is first created.
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @Id
    private UUID id;

    // Each OAuth identity belongs to one Finance Tracker user.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OAuthProvider provider;

    // The stable user identifier supplied by the OAuth provider.
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}