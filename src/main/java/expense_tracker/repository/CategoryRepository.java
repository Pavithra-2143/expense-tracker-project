package expense_tracker.repository;

import expense_tracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Retrieves all categories belonging to a specific user.
     * Not paginated — a user's category set is small and bounded by nature,
     * unlike the ever-growing Expense table, so pagination would be
     * unjustified complexity here.
     */
    List<Category> findByUserId(Long userId);

    /**
     * Checks whether a user already has a category with the given name.
     * Mirrors the (user_id, name) unique constraint enforced at the database
     * level, letting the service layer reject duplicates before attempting
     * an insert or rename.
     */
    boolean existsByUserIdAndName(Long userId, String name);

    /**
     * Retrieves a category by its id, scoped to the owning user.
     * <p>
     * Deliberately combines both conditions in a single query rather than
     * exposing plain findById(id) for service-layer use: this makes it
     * structurally impossible to fetch a category without also verifying
     * ownership, closing off a class of IDOR (Insecure Direct Object
     * Reference) bugs where a user could access another user's category
     * by guessing or reusing an id.
     */
    Optional<Category> findByIdAndUserId(Long id, Long userId);
}