package com.kartik.finance_tracker.users;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.AbstractPostgresIntegrationTest;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {

        User user = new User(
                "integration@example.com",
                "hashed-password",
                "Integration User"
        );

        userRepository.save(user);

        // Email is the user's unique login identifier,
        // so the repository must be able to retrieve the correct user by email.
        Optional<User> result =
                userRepository.findByEmail("integration@example.com");

        assertThat(result)
                .isPresent();

        assertThat(result.get().getEmail())
                .isEqualTo("integration@example.com");

        assertThat(result.get().getName())
                .isEqualTo("Integration User");
    }

    @Test
    void shouldCheckWhetherEmailExists() {

        User user = new User(
                "existing@example.com",
                "hashed-password",
                "Existing User"
        );

        userRepository.save(user);

        // Registration must be able to detect an already-used email
        // before creating another native user with the same email.
        assertThat(
                userRepository.existsByEmail("existing@example.com")
        ).isTrue();

        assertThat(
                userRepository.existsByEmail("missing@example.com")
        ).isFalse();
    }
}
