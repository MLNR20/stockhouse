package com.example.warehouse_proj.category;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CategoryService {

    private static final String ENTITY = "Category";
    private static final Set<String> SORT_FIELDS = Set.of("name", "description");
    private static final List<String> SEARCH_FIELDS = List.of(
            "name", "description");

    private final CategoryRepository categories;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public CategoryService(
            CategoryRepository categories,
            AuditLogService audit,
            PaginationService pagination) {
        this.categories = categories;
        this.audit = audit;
        this.pagination = pagination;
    }

    /** Active categories; with includeInactive the soft-deleted ones too (their products still show them). */
    @Transactional(readOnly = true)
    public List<CategoryDtos.Response> list(boolean includeInactive) {
        return (includeInactive ? categories.findAll() : categories.findAllByActiveTrue()).stream()
                .map(CategoryDtos.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryDtos.Response> page(PageQuery query) {
        return pagination.page(categories, query, SORT_FIELDS, SEARCH_FIELDS, CategoryDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public CategoryDtos.Response get(UUID id) {
        return CategoryDtos.Response.from(find(id));
    }

    @Transactional
    public CategoryDtos.Response create(CategoryDtos.Request request) {
        Category category = new Category(request.name().trim(), blankToNull(request.description()));
        categories.save(category);
        audit.record(AuditAction.CREATE, ENTITY, category.getId());
        return CategoryDtos.Response.from(category);
    }

    @Transactional
    public CategoryDtos.Response update(UUID id, CategoryDtos.Request request) {
        Category category = find(id);
        category.setName(request.name().trim());
        category.setDescription(blankToNull(request.description()));
        // Flush so @PreUpdate has set updatedAt before the response is built.
        categories.saveAndFlush(category);
        audit.record(AuditAction.UPDATE, ENTITY, category.getId());
        return CategoryDtos.Response.from(category);
    }

    /**
     * Soft delete. Products keep pointing at the category, and it stays visible on them (marked as deleted),
     * so deleting a category never changes or hides what a product was filed under.
     */
    @Transactional
    public void delete(UUID id) {
        Category category = find(id);
        category.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private Category find(UUID id) {
        return categories.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
