package com.example.warehouse_proj.employee;

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
@RequestMapping("/api/warehouse-employees")
public class WarehouseEmployeeController {

    private final WarehouseEmployeeService service;

    public WarehouseEmployeeController(WarehouseEmployeeService service) {
        this.service = service;
    }

    /** Optionally filter by warehouse: GET /api/warehouse-employees?warehouseId=... */
    @GetMapping
    public List<WarehouseEmployeeDtos.Response> list(@RequestParam(required = false) UUID warehouseId) {
        return service.list(warehouseId);
    }

    /** Paged: GET /api/warehouse-employees/page?page=0&size=20&sortBy=lastName&direction=asc */
    @GetMapping("/page")
    public PageResponse<WarehouseEmployeeDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public WarehouseEmployeeDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseEmployeeDtos.Response create(@Valid @RequestBody WarehouseEmployeeDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WarehouseEmployeeDtos.Response update(
            @PathVariable UUID id, @Valid @RequestBody WarehouseEmployeeDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
