package com.example.warehouse_proj.orderdetail;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Lines of an order live under it for listing and adding, and under /api/order-details for the rest. */
@RestController
@RequestMapping("/api")
public class OrderDetailController {

    private final OrderDetailService service;

    public OrderDetailController(OrderDetailService service) {
        this.service = service;
    }

    @GetMapping("/orders/{orderId}/details")
    public List<OrderDetailDtos.Response> list(@PathVariable UUID orderId) {
        return service.list(orderId);
    }

    @PostMapping("/orders/{orderId}/details")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailDtos.Response create(
            @PathVariable UUID orderId, @Valid @RequestBody OrderDetailDtos.Request request) {
        return service.create(orderId, request);
    }

    /** Paged across all orders: GET /api/order-details/page?page=0&size=20&sortBy=quantity&direction=desc */
    @GetMapping("/order-details/page")
    public PageResponse<OrderDetailDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/order-details/{id}")
    public OrderDetailDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/order-details/{id}")
    public OrderDetailDtos.Response update(
            @PathVariable UUID id, @Valid @RequestBody OrderDetailDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/order-details/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
