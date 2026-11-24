package com.example.warehouse_proj.shipment;

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
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    /** Optionally filter: GET /api/shipments?orderId=...&truckId=... */
    @GetMapping
    public List<ShipmentDtos.Response> list(
            @RequestParam(required = false) UUID orderId, @RequestParam(required = false) UUID truckId) {
        return service.list(orderId, truckId);
    }

    /** Paged: GET /api/shipments/page?page=0&size=20&sortBy=scheduledDate&direction=asc */
    @GetMapping("/page")
    public PageResponse<ShipmentDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public ShipmentDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentDtos.Response create(@Valid @RequestBody ShipmentDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ShipmentDtos.Response update(@PathVariable UUID id, @Valid @RequestBody ShipmentDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
