package com.kartik.finance_tracker.auth;

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
@Table(name = "oauth_handoff_codes")
public class OAuthHandoffCode {

    public OAuthHandoffCode(
            User user,
            String codeHash,
            String sessionIdHash,
            OffsetDateTime expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.codeHash = codeHash;
        this.sessionIdHash = sessionIdHash;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Column(name = "session_id_hash", nullable = false, length = 64)
    private String sessionIdHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
