package expense_tracker.service;

import expense_tracker.entity.Category;
import expense_tracker.entity.User;
import expense_tracker.repository.CategoryRepository;
import expense_tracker.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import java.util.Locale;

@Service
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Transactional
    public Category createCategory(Long userId, String name) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Category name must not be empty");
        }

        String normalizedName = name.trim();
        User user = userService.getUserById(userId);

        boolean exists = categoryRepository.existsByUserIdAndName(userId, normalizedName);

        System.out.println("USER ID = " + userId);
        System.out.println("CATEGORY = " + normalizedName);
        System.out.println("EXISTS = " + exists);

        if (exists) {
            log.warn("Category creation rejected - duplicate name '{}' for user id {}",
                    sanitizeForLog(normalizedName), userId);
            throw new IllegalArgumentException("Category already exists: " + normalizedName);
        }

        Category category = new Category();
        category.setName(normalizedName);
        category.setUser(user);

        try {
            Category saved = categoryRepository.save(category);
            log.info("Category '{}' created for user id {}", sanitizeForLog(normalizedName), userId);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Category creation rejected - concurrent duplicate name '{}' for user id {}",
                    sanitizeForLog(normalizedName), userId);
            // TODO: replace with custom DuplicateCategoryException once exception layer exists.
            throw new IllegalArgumentException("Category already exists: " + normalizedName, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        return categoryRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Category getCategoryForUser(Long categoryId, Long userId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category id must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> {
                    log.warn("Category lookup failed - id {} not found for user id {}", categoryId, userId);
                    // TODO: replace with custom CategoryNotFoundException once exception layer exists.
                    return new ResourceNotFoundException("No category found with id: " + categoryId);
                });
    }

    @Transactional
    public Category renameCategory(Long categoryId, Long userId, String newName) {
        if (!StringUtils.hasText(newName)) {
            throw new IllegalArgumentException("Category name must not be empty");
        }

        String normalizedName = newName.trim();
        Category category = getCategoryForUser(categoryId, userId);

        if (!category.getName().equals(normalizedName)
                && categoryRepository.existsByUserIdAndName(userId, normalizedName)) {
            log.warn("Category rename rejected - duplicate name '{}' for user id {}",
                    sanitizeForLog(normalizedName), userId);
            throw new IllegalArgumentException("Category already exists: " + normalizedName);
        }

        category.setName(normalizedName);

        try {
            Category saved = categoryRepository.save(category);
            log.info("Category id {} renamed to '{}' for user id {}", categoryId, sanitizeForLog(normalizedName), userId);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Category rename rejected - concurrent duplicate name '{}' for user id {}",
                    sanitizeForLog(normalizedName), userId);
            throw new IllegalArgumentException("Category already exists: " + normalizedName, ex);
        }
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = getCategoryForUser(categoryId, userId);

        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
            log.info("Category id {} deleted for user id {}", categoryId, userId);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Category deletion rejected - category id {} still referenced by existing expenses", categoryId);
            // TODO: replace with custom CategoryInUseException once exception layer exists.
            throw new IllegalStateException(
                    "Cannot delete category: it is still referenced by existing expenses", ex);
        }
    }

    private String sanitizeForLog(String input) {
        return input.replaceAll("[\\r\\n]", "_").toLowerCase(Locale.ROOT);
    }
}
