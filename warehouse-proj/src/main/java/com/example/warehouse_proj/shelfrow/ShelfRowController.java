package com.example.warehouse_proj.shelfrow;

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
@RequestMapping("/api/rows")
public class ShelfRowController {

    private final ShelfRowService service;

    public ShelfRowController(ShelfRowService service) {
        this.service = service;
    }

    /** Optionally filter by shelf: GET /api/rows?shelfId=... */
    @GetMapping
    public List<ShelfRowDtos.Response> list(@RequestParam(required = false) UUID shelfId) {
        return service.list(shelfId);
    }

    /** Paged: GET /api/rows/page?page=0&size=20&sortBy=name&direction=asc */
    @GetMapping("/page")
    public PageResponse<ShelfRowDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public ShelfRowDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShelfRowDtos.Response create(@Valid @RequestBody ShelfRowDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ShelfRowDtos.Response update(@PathVariable UUID id, @Valid @RequestBody ShelfRowDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
