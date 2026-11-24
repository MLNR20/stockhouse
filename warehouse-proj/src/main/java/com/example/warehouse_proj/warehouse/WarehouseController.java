package com.example.warehouse_proj.warehouse;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping
    public List<WarehouseDtos.Response> list() {
        return service.list();
    }

    /** Paged: GET /api/warehouses/page?page=0&size=20&sortBy=name&direction=asc */
    @GetMapping("/page")
    public PageResponse<WarehouseDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public WarehouseDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseDtos.Response create(@Valid @RequestBody WarehouseDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WarehouseDtos.Response update(@PathVariable UUID id, @Valid @RequestBody WarehouseDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
