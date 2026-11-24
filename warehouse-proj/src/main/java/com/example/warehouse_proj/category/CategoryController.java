package com.example.warehouse_proj.category;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    /** Optionally include soft-deleted categories: GET /api/categories?includeInactive=true */
    @GetMapping
    public List<CategoryDtos.Response> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(includeInactive);
    }

    /** Paged: GET /api/categories/page?page=0&size=20&sortBy=name&direction=asc */
    @GetMapping("/page")
    public PageResponse<CategoryDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public CategoryDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDtos.Response create(@Valid @RequestBody CategoryDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CategoryDtos.Response update(@PathVariable UUID id, @Valid @RequestBody CategoryDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
