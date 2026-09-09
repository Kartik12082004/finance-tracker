package com.kartik.finance_tracker.users;

import org.springframework.stereotype.Service;

@Service 
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
     public User createUser(String email, String passwordHash, String name) {
        User user = new User(email, passwordHash, name);

        return userRepository.save(user);
    }

}
