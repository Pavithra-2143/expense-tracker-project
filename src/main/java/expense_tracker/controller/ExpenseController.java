package expense_tracker.controller;
import expense_tracker.repository.MonthlyExpenseSummary;
import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import expense_tracker.entity.Expense;
import expense_tracker.service.ExpenseService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;

@RestController
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/users/{userId}/expenses")
@Slf4j
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @PathVariable Long userId,
            @jakarta.validation.Valid @RequestBody ExpenseRequest request) {
        Expense created = expenseService.createExpense(
                userId, request.categoryId(), request.amount(), request.description(), request.expenseDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(ExpenseResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> getExpenses(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<ExpenseResponse> page = expenseService.getExpenses(userId, pageable)
                .map(ExpenseResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ExpenseResponse>> getExpensesByCategory(
            @PathVariable Long userId,
            @PathVariable Long categoryId,
            Pageable pageable) {
        Page<ExpenseResponse> page = expenseService.getExpensesByCategory(userId, categoryId, pageable)
                .map(ExpenseResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/range")
    public ResponseEntity<Page<ExpenseResponse>> getExpensesByDateRange(
            @PathVariable Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Pageable pageable) {
        Page<ExpenseResponse> page = expenseService.getExpensesByDateRange(userId, startDate, endDate, pageable)
                .map(ExpenseResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/total")
    public ResponseEntity<TotalResponse> getTotalSpent(@PathVariable Long userId) {
        BigDecimal total = expenseService.getTotalSpent(userId);
        return ResponseEntity.ok(new TotalResponse(total));
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<MonthlyExpenseSummary>> getMonthlyExpenseSummary(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                expenseService.getMonthlyExpenseSummary(userId)
        );
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(
            @PathVariable Long userId,
            @PathVariable Long expenseId) {
        Expense expense = expenseService.getExpenseForUser(expenseId, userId);
        return ResponseEntity.ok(ExpenseResponse.from(expense));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long userId,
            @PathVariable Long expenseId,
            @RequestBody ExpenseUpdateRequest request) {
        Expense updated = expenseService.updateExpense(
                expenseId, userId, request.categoryId(), request.amount(),
                request.description(), request.expenseDate());
        return ResponseEntity.ok(ExpenseResponse.from(updated));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long userId,
            @PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, userId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    public record ExpenseRequest(
            @NotNull Long categoryId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @Size(max = 255) String description,
            @NotNull LocalDate expenseDate
    ) {}

    public record ExpenseUpdateRequest(
            Long categoryId,
            @DecimalMin(value = "0.01") BigDecimal amount,
            @Size(max = 255) String description,
            LocalDate expenseDate
    ) {}

    public record ExpenseResponse(
            Long id,
            BigDecimal amount,
            String description,
            LocalDate expenseDate,
            Long categoryId,
            String categoryName
    ) {
        static ExpenseResponse from(Expense expense) {
            return new ExpenseResponse(
                    expense.getId(),
                    expense.getAmount(),
                    expense.getDescription(),
                    expense.getExpenseDate(),
                    expense.getCategory() != null ? expense.getCategory().getId() : null,
                    expense.getCategory() != null ? expense.getCategory().getName() : null
            );
        }
    }

    public record TotalResponse(BigDecimal total) {}

    public record ErrorResponse(String message) {}
}
