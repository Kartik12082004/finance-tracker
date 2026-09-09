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
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserService(userRepository);

        User savedUser = new User(
            "kartik@example.com",
            "hashedpassword",
            "Kartik"
        );

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.createUser(
            "kartik@example.com",
            "hashedpassword",
            "Kartik"
        );

        assertThat(result).isSameAs(savedUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User userPassedToRepository = userCaptor.getValue();

        assertThat(userPassedToRepository.getEmail())
                .isEqualTo("kartik@example.com");
        assertThat(userPassedToRepository.getPasswordHash())
                .isEqualTo("hashedpassword");
        assertThat(userPassedToRepository.getName())
                .isEqualTo("Kartik");
        assertThat(userPassedToRepository.getId())
                .isNotNull();
        assertThat(userPassedToRepository.getCreatedAt())
                .isNotNull();
        assertThat(userPassedToRepository.getUpdatedAt())
                .isNotNull();

    }

}