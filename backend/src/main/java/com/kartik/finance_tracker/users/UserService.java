package com.kartik.finance_tracker.users;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String email, String passwordHash, String name) {

        // Create the user entity before passing it to the repository.
        User user = new User(email, passwordHash, name);

        // Persist the newly created user and return the saved entity.
        return userRepository.save(user);
    }
}