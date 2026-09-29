package com.kartik.finance_tracker.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUser_Id(UUID userId);

    // Only return an account when it belongs to the authenticated user.
    Optional<Account> findByIdAndUser_Id(UUID accountId, UUID userId);
}
