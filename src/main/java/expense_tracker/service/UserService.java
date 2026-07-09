package expense_tracker.service;

import expense_tracker.entity.User;
import expense_tracker.repository.UserRepository;
import expense_tracker.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Business logic for User accounts: registration (duplicate-email
 * prevention, password hashing) and lookups by email or id.
 * <p>
 * NOTE: Accepts/returns the User entity directly. Not yet safe to expose
 * via a public controller (mass-assignment risk) until a registration
 * DTO exists at the boundary.
 */
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user: validates input, normalizes email, rejects
     * duplicates, hashes the password, and persists the account.
     * The existsByEmail check is a fast-path only; the DB unique
     * constraint is the real source of truth under concurrent requests.
     */
    @Transactional
    public User registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("Email must not be empty");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Password must not be empty");
        }

        String normalizedEmail = user.getEmail().trim().toLowerCase(Locale.ROOT);
        log.info("Attempting to register user with email: {}", sanitizeForLog(normalizedEmail));

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected - email already in use: {}", sanitizeForLog(normalizedEmail));
            // TODO: replace with custom DuplicateEmailException once exception layer exists.
            throw new IllegalArgumentException("Email is already registered: " + normalizedEmail);
        }

        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Registration rejected - concurrent duplicate email detected: {}",
                    sanitizeForLog(normalizedEmail));
            // TODO: replace with custom DuplicateEmailException once exception layer exists.
            throw new IllegalArgumentException("Email is already registered: " + normalizedEmail, ex);
        }

        log.info("User registered successfully with id: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Retrieves a user by email. Used by authentication flows.
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email must not be empty");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("User lookup failed - no user found with email: {}", sanitizeForLog(normalizedEmail));
                    // TODO: replace with custom UserNotFoundException once exception layer exists.
                    return new ResourceNotFoundException("No user found with email: " + normalizedEmail);
                });
    }

    /**
     * Retrieves a user by id. Used to resolve "the current authenticated
     * user" for downstream operations once a security context exists.
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User lookup failed - no user found with id: {}", id);
                    return new ResourceNotFoundException("No user found with id: " + id);
                });
    }

    /**
     * Strips CR/LF from user-supplied input before logging, preventing
     * log-forging (CWE-117).
     */
    private String sanitizeForLog(String input) {
        return input.replaceAll("[\\r\\n]", "_");
    }
}
