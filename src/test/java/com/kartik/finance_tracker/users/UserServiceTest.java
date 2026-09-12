package com.kartik.finance_tracker.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class UserServiceTest {

    @Test
    void createUser_shouldSaveAndReturnUser() {

        // Create mocked dependencies so this test focuses only on UserService
        // and does not require a real database.
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserService(userRepository);

        // Represent the user that would be returned after the repository saves it.
        User savedUser = new User(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // Create a user through the service.
        User result = userService.createUser(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );

        // The service should return the user produced by the repository.
        assertThat(result).isSameAs(savedUser);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        // Capture the user passed to the repository so we can verify
        // that the service constructed it with the correct values.
        verify(userRepository).save(userCaptor.capture());

        User userPassedToRepository = userCaptor.getValue();

        // Verify that the supplied user details are preserved.
        assertThat(userPassedToRepository.getEmail())
                .isEqualTo("kartik@example.com");

        assertThat(userPassedToRepository.getPasswordHash())
                .isEqualTo("hashedpassword");

        assertThat(userPassedToRepository.getName())
                .isEqualTo("Kartik");

        // Entity construction should generate the ID and timestamps.
        assertThat(userPassedToRepository.getId())
                .isNotNull();

        assertThat(userPassedToRepository.getCreatedAt())
                .isNotNull();

        assertThat(userPassedToRepository.getUpdatedAt())
                .isNotNull();
    }
}