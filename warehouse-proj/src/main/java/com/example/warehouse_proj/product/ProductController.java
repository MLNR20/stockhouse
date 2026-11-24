package com.example.warehouse_proj.product;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** Optionally filter by category: GET /api/products?categoryId=... */
    @GetMapping
    public List<ProductDtos.Response> list(@RequestParam(required = false) UUID categoryId) {
        return service.list(categoryId);
    }

    /** Paged: GET /api/products/page?page=0&size=20&sortBy=name&direction=asc */
    @GetMapping("/page")
    public PageResponse<ProductDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public ProductDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDtos.Response create(@Valid @RequestBody ProductDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ProductDtos.Response update(@PathVariable UUID id, @Valid @RequestBody ProductDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
