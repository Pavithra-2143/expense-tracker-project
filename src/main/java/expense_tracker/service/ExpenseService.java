package expense_tracker.service;
import java.util.List;
import expense_tracker.repository.MonthlyExpenseSummary;
import expense_tracker.entity.Category;
import expense_tracker.entity.Expense;
import expense_tracker.entity.User;
import expense_tracker.repository.ExpenseRepository;
import expense_tracker.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;


@Service
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          UserService userService,
                          CategoryService categoryService) {
        this.expenseRepository = expenseRepository;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Transactional
    public Expense createExpense(Long userId, Long categoryId, BigDecimal amount,
                                 String description, LocalDate expenseDate) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Category id must not be null");
        }
        validateAmount(amount);
        if (expenseDate == null) {
            throw new IllegalArgumentException("Expense date must not be null");
        }
        if (expenseDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expense date must not be in the future");
        }

        User user = userService.getUserById(userId);
        // Ownership of the category is enforced here: getCategoryForUser only
        // returns a Category if it belongs to this exact user, preventing a
        // user from attaching another user's category id to their expense.
        Category category = categoryService.getCategoryForUser(categoryId, userId);

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setDescription(description != null ? description.trim() : null);
        expense.setExpenseDate(expenseDate);

        Expense saved = expenseRepository.save(expense);
        log.info("Expense id {} created for user id {} in category id {}", saved.getId(), userId, categoryId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpenses(Long userId, Pageable pageable) {
        requireUserId(userId);
        requirePageable(pageable);
        return expenseRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByCategory(Long userId, Long categoryId, Pageable pageable) {
        requireUserId(userId);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category id must not be null");
        }
        requirePageable(pageable);
        // Confirms the category belongs to this user before filtering by it,
        // preventing disclosure of another user's category-scoped expenses.
        categoryService.getCategoryForUser(categoryId, userId);
        return expenseRepository.findByUserIdAndCategoryId(userId, categoryId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        requireUserId(userId);
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        requirePageable(pageable);
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Expense getExpenseForUser(Long expenseId, Long userId) {
        if (expenseId == null) {
            throw new IllegalArgumentException("Expense id must not be null");
        }
        requireUserId(userId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> {
                    log.warn("Expense lookup failed - id {} not found", expenseId);
                    // TODO: replace with custom ExpenseNotFoundException once exception layer exists.
                    return new ResourceNotFoundException("No expense found with id: " + expenseId);
                });

        assertOwnership(expense, userId);
        return expense;
    }

    @Transactional
    public Expense updateExpense(Long expenseId, Long userId, Long categoryId,
                                 BigDecimal amount, String description, LocalDate expenseDate) {
        Expense expense = getExpenseForUser(expenseId, userId);

        if (categoryId != null) {
            Category category = categoryService.getCategoryForUser(categoryId, userId);
            expense.setCategory(category);
        }
        if (amount != null) {
            validateAmount(amount);
            expense.setAmount(amount);
        }
        if (description != null) {
            expense.setDescription(description.trim());
        }
        if (expenseDate != null) {
            if (expenseDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Expense date must not be in the future");
            }
            expense.setExpenseDate(expenseDate);
        }

        Expense saved = expenseRepository.save(expense);
        log.info("Expense id {} updated for user id {}", expenseId, userId);
        return saved;
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        Expense expense = getExpenseForUser(expenseId, userId);
        expenseRepository.delete(expense);
        log.info("Expense id {} deleted for user id {}", expenseId, userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSpent(Long userId) {
        requireUserId(userId);
        BigDecimal total = expenseRepository.sumAmountByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<MonthlyExpenseSummary> getMonthlyExpenseSummary(Long userId) {
        requireUserId(userId);
        return expenseRepository.getMonthlyExpenseSummary(userId);
    }

    private void assertOwnership(Expense expense, Long userId) {
        if (expense.getUser() == null || !expense.getUser().getId().equals(userId)) {
            log.warn("Ownership check failed - expense id {} does not belong to user id {}",
                    expense.getId(), userId);
            // Returning "not found" rather than "forbidden" avoids confirming
            // the expense's existence to a non-owning caller (IDOR mitigation).
            throw new ResourceNotFoundException("No expense found with id: " + expense.getId());
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
    }

    private void requirePageable(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable must not be null");
        }
    }
}