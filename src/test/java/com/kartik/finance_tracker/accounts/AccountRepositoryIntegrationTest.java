package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.AbstractPostgresIntegrationTest;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@SpringBootTest
@Transactional
class AccountRepositoryIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindAccountByIdForOwningUser() {

        User user = new User(
                "owner@example.com",
                "hashed-password",
                "Owner"
        );

        userRepository.save(user);

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        accountRepository.save(account);

        // An authenticated user should be able to retrieve an account
        // when both the account ID and user ID belong together.
        Optional<Account> result =
                accountRepository.findByIdAndUser_Id(
                        account.getId(),
                        user.getId()
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().getId())
                .isEqualTo(account.getId());

        assertThat(result.get().getUser().getId())
                .isEqualTo(user.getId());
    }

    @Test
    void shouldNotFindAccountForDifferentUser() {

        User owner = new User(
                "owner@example.com",
                "hashed-password",
                "Owner"
        );

        User otherUser = new User(
                "other@example.com",
                "hashed-password",
                "Other User"
        );

        userRepository.save(owner);
        userRepository.save(otherUser);

        Account account = new Account(
                owner,
                "SBI Savings",
                AccountType.BANK,
                "INR"
        );

        accountRepository.save(account);

        // Changing the user ID must not allow another user to access
        // an account that they do not own.
        Optional<Account> result =
                accountRepository.findByIdAndUser_Id(
                        account.getId(),
                        otherUser.getId()
                );

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldFindOnlyAccountsBelongingToUser() {

        User user = new User(
                "owner@example.com",
                "hashed-password",
                "Owner"
        );

        User otherUser = new User(
                "other@example.com",
                "hashed-password",
                "Other User"
        );

        userRepository.save(user);
        userRepository.save(otherUser);

        Account firstAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Account secondAccount = new Account(
                user,
                "Cash",
                AccountType.CASH,
                "INR"
        );

        Account otherAccount = new Account(
                otherUser,
                "SBI Savings",
                AccountType.BANK,
                "INR"
        );

        accountRepository.saveAll(
                List.of(firstAccount, secondAccount, otherAccount)
        );

        // Account listings must be scoped to the authenticated user.
        // Another user's accounts must never appear in the result.
        List<Account> results =
                accountRepository.findByUser_Id(user.getId());

        assertThat(results)
                .hasSize(2)
                .extracting(Account::getId)
                .containsExactlyInAnyOrder(
                        firstAccount.getId(),
                        secondAccount.getId()
                );

        assertThat(results)
                .extracting(account -> account.getUser().getId())
                .containsOnly(user.getId());

        // Make sure the other user's account was not accidentally returned.
        assertThat(results)
                .extracting(Account::getId)
                .doesNotContain(otherAccount.getId());
    }
}
