package com.kartik.finance_tracker.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthHandoffCodeRepository
        extends JpaRepository<OAuthHandoffCode, UUID> {

    @Modifying
    @Query("""
            UPDATE OAuthHandoffCode code
            SET code.consumedAt = :now
            WHERE code.codeHash = :codeHash
              AND code.sessionIdHash = :sessionIdHash
              AND code.consumedAt IS NULL
              AND code.expiresAt > :now
            """)
    int consumeIfValid(
            @Param("codeHash") String codeHash,
            @Param("sessionIdHash") String sessionIdHash,
            @Param("now") OffsetDateTime now
    );

    Optional<OAuthHandoffCode> findByCodeHash(String codeHash);
}
