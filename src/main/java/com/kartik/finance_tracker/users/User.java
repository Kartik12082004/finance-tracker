package com.kartik.finance_tracker.users;

import com.kartik.finance_tracker.accounts.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

    public User(String email, String passwordHash, String name) {

        // Generate the user ID in the application.
        this.id = UUID.randomUUID();

        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;

        // Set both timestamps when the user is first created.
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // Store the hashed password rather than the user's plain-text password.
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // A user can own multiple accounts.
    @OneToMany(mappedBy = "user")
    private List<Account> accounts = new ArrayList<>();
}