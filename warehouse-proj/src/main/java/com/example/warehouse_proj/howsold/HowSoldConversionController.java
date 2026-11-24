package com.example.warehouse_proj.howsold;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/how-sold-conversions")
public class HowSoldConversionController {

    private final HowSoldConversionService service;

    public HowSoldConversionController(HowSoldConversionService service) {
        this.service = service;
    }

    /** Optionally only one product's conversions: GET /api/how-sold-conversions?productId=... */
    @GetMapping
    public List<HowSoldConversionDtos.Response> list(@RequestParam(required = false) UUID productId) {
        return service.list(productId);
    }

    /** Paged: GET /api/how-sold-conversions/page?page=0&size=20&sortBy=unitConversion&direction=asc */
    @GetMapping("/page")
    public PageResponse<HowSoldConversionDtos.Response> page(@ModelAttribute PageQuery query) {
        return service.page(query);
    }

    @GetMapping("/{id}")
    public HowSoldConversionDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HowSoldConversionDtos.Response create(@Valid @RequestBody HowSoldConversionDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public HowSoldConversionDtos.Response update(
            @PathVariable UUID id, @Valid @RequestBody HowSoldConversionDtos.Request request) {
        return service.update(id, request);
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
