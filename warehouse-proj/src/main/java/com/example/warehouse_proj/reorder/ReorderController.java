package com.example.warehouse_proj.reorder;

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
@RequestMapping("/api/reorders")
public class ReorderController {

    private final ReorderService service;

    public ReorderController(ReorderService service) {
        this.service = service;
    }

    /** Optionally filter: GET /api/reorders?supplierId=...&productId=... */
    @GetMapping
    public List<ReorderDtos.Response> list(
            @RequestParam(required = false) UUID supplierId, @RequestParam(required = false) UUID productId) {
        return service.list(supplierId, productId);
    }

    /** Paged: GET /api/reorders/page?page=0&size=20&sortBy=expectedDate&direction=asc */
    @GetMapping("/page")
    public PageResponse<ReorderDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public ReorderDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReorderDtos.Response create(@Valid @RequestBody ReorderDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ReorderDtos.Response update(@PathVariable UUID id, @Valid @RequestBody ReorderDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
