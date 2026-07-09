package expense_tracker.repository;

import expense_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Used during authentication and profile lookups.
     * Returns Optional to force explicit handling of the "not found" case.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email already exists.
     * Used during registration to reject duplicate sign-ups efficiently,
     * without fetching the full entity.
     */
    boolean existsByEmail(String email);
}