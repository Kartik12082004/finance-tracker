package com.kartik.finance_tracker.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.AbstractPostgresIntegrationTest;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@SpringBootTest
@Transactional
class RefreshTokenRepositoryIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindRefreshTokenByHash() {

        User user = new User(
                "refresh@example.com",
                "hashed-password",
                "Refresh User"
        );

        userRepository.save(user);

        String tokenHash =
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                OffsetDateTime.now().plusDays(30)
        );

        refreshTokenRepository.save(refreshToken);

        // The raw refresh token is never stored.
        // The repository looks up the server-side SHA-256 hash instead.
        Optional<RefreshToken> result =
                refreshTokenRepository.findByTokenHash(tokenHash);

        assertThat(result)
                .isPresent();

        assertThat(result.get().getId())
                .isEqualTo(refreshToken.getId());

        assertThat(result.get().getUser().getId())
                .isEqualTo(user.getId());

        assertThat(result.get().getTokenHash())
                .isEqualTo(tokenHash);
    }

    @Test
    void shouldEnforceUniqueTokenHash() {

        User user = new User(
                "unique-refresh@example.com",
                "hashed-password",
                "Refresh User"
        );

        userRepository.save(user);

        String tokenHash =
                "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

        RefreshToken firstToken = new RefreshToken(
                user,
                tokenHash,
                OffsetDateTime.now().plusDays(30)
        );

        RefreshToken secondToken = new RefreshToken(
                user,
                tokenHash,
                OffsetDateTime.now().plusDays(30)
        );

        refreshTokenRepository.saveAndFlush(firstToken);

        // A token hash is unique because two refresh-token records
        // must never represent the same refresh credential.
        assertThatThrownBy(() ->
                refreshTokenRepository.saveAndFlush(secondToken)
        )
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
