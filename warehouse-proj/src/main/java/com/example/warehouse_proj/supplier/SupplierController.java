package com.example.warehouse_proj.supplier;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierDtos.Response> list() {
        return service.list();
    }

    /** Paged: GET /api/suppliers/page?page=0&size=20&sortBy=supplierName&direction=asc */
    @GetMapping("/page")
    public PageResponse<SupplierDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public SupplierDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDtos.Response create(@Valid @RequestBody SupplierDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SupplierDtos.Response update(@PathVariable UUID id, @Valid @RequestBody SupplierDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PutMapping("/{supplierId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addProduct(@PathVariable UUID supplierId, @PathVariable UUID productId) {
        service.addProduct(supplierId, productId);
    }

    @DeleteMapping("/{supplierId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProduct(@PathVariable UUID supplierId, @PathVariable UUID productId) {
        service.removeProduct(supplierId, productId);
    }
}
