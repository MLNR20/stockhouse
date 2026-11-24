package com.example.warehouse_proj.container;

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
@RequestMapping("/api/containers")
public class ContainerController {

    private final ContainerService service;

    public ContainerController(ContainerService service) {
        this.service = service;
    }

    /** Optionally filter by row: GET /api/containers?rowId=... */
    @GetMapping
    public List<ContainerDtos.Response> list(@RequestParam(required = false) UUID rowId) {
        return service.list(rowId);
    }

    /** Paged: GET /api/containers/page?page=0&size=20&sortBy=name&direction=asc */
    @GetMapping("/page")
    public PageResponse<ContainerDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public ContainerDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContainerDtos.Response create(@Valid @RequestBody ContainerDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ContainerDtos.Response update(@PathVariable UUID id, @Valid @RequestBody ContainerDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}/products")
    public List<ContainerDtos.ContentResponse> listContents(@PathVariable UUID id) {
        return service.listContents(id);
    }

    /** Put a product in the container, or change its quantity. */
    @PutMapping("/{id}/products/{productId}")
    public ContainerDtos.ContentResponse putContent(
            @PathVariable UUID id,
            @PathVariable UUID productId,
            @Valid @RequestBody ContainerDtos.ContentRequest request) {
        return service.putContent(id, productId, request.quantity());
    }

    /** Soft-removes a product from the container. */
    @DeleteMapping("/{id}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeContent(@PathVariable UUID id, @PathVariable UUID productId) {
        service.removeContent(id, productId);
    }
}
