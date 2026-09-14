package com.kartik.finance_tracker.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kartik.finance_tracker.auth.dto.AuthResponse;
import com.kartik.finance_tracker.auth.dto.LoginRequest;
import com.kartik.finance_tracker.auth.dto.RegisterRequest;
import com.kartik.finance_tracker.auth.jwt.JwtService;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        // Each email can belong to only one Finance Tracker user.
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Hash the password before creating the user.
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.email(),
                passwordHash,
                request.name()
        );

        User savedUser = userRepository.save(user);

        // Issue an access token immediately after successful registration.
        String accessToken = jwtService.generateAccessToken(savedUser.getId());

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                accessToken,
                jwtService.getAccessTokenExpiration()
        );
    }

    public AuthResponse login(LoginRequest request) {

        // Find the user associated with the supplied email.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or password"));

        // Compare the supplied raw password against the stored BCrypt hash.
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Issue an access token after successful authentication.
        String accessToken = jwtService.generateAccessToken(user.getId());

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                accessToken,
                jwtService.getAccessTokenExpiration()
        );
    }
}