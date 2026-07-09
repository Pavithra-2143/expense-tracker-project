package expense_tracker.controller;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import expense_tracker.entity.Category;
import expense_tracker.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/users/{userId}/categories")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @PathVariable Long userId,
            @Valid @RequestBody CategoryRequest request) {
        Category created = categoryService.createCategory(userId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(@PathVariable Long userId) {
        List<CategoryResponse> categories = categoryService.getCategoriesForUser(userId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(
            @PathVariable Long userId,
            @PathVariable Long categoryId) {
        Category category = categoryService.getCategoryForUser(categoryId, userId);
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> renameCategory(
            @PathVariable Long userId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        Category renamed = categoryService.renameCategory(categoryId, userId, request.name());
        return ResponseEntity.ok(CategoryResponse.from(renamed));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long userId,
            @PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId, userId);
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

    public record CategoryRequest(@NotBlank @Size(max = 50) String name) {}

    public record CategoryResponse(Long id, String name) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName());
        }
    }

    public record ErrorResponse(String message) {}
}