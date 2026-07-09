package expense_tracker.repository;
import java.util.List;
import expense_tracker.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Retrieves a paginated list of expenses belonging to a specific user.
     * Pagination is mandatory here, not optional — this table grows unbounded
     * over a user's lifetime, so returning an unpaginated List would risk
     * unbounded memory use and slow responses at scale.
     */
    Page<Expense> findByUserId(Long userId, Pageable pageable);

    /**
     * Retrieves a paginated list of a user's expenses filtered by category.
     * Backs category-specific views (e.g. "all Food expenses").
     */
    Page<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);

    /**
     * Retrieves a paginated list of a user's expenses within a date range.
     * Backs reporting features such as monthly or custom-range summaries.
     */
    Page<Expense> findByUserIdAndExpenseDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    /**
     * Computes the total amount spent by a user, using a database-level
     * aggregate rather than fetching all rows into the JVM to sum them.
     * <p>
     * Returns {@code null} if the user has no expenses at all — JPQL's SUM()
     * returns null (not zero) over an empty result set. Callers must handle
     * this explicitly rather than assuming a non-null result.
     */
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);

@Query("""
SELECT
YEAR(e.expenseDate) AS year,
MONTH(e.expenseDate) AS month,
SUM(e.amount) AS total
FROM Expense e
WHERE e.user.id = :userId
GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate)
ORDER BY YEAR(e.expenseDate), MONTH(e.expenseDate)
""")
List<MonthlyExpenseSummary> getMonthlyExpenseSummary(@Param("userId") Long userId);
}
