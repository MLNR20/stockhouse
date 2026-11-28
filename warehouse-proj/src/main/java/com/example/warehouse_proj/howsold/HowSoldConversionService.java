package com.example.warehouse_proj.howsold;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class HowSoldConversionService {

    private static final String ENTITY = "HowSoldConversion";
    private static final Set<String> SORT_FIELDS = Set.of("unitConversion");
    private static final List<String> SEARCH_FIELDS = List.of(
            "parentHowSold.name", "childHowSold.name", "product.name");

    private final HowSoldConversionRepository conversions;
    private final HowSoldRepository howSolds;
    private final ProductRepository products;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public HowSoldConversionService(
            HowSoldConversionRepository conversions,
            HowSoldRepository howSolds,
            ProductRepository products,
            AuditLogService audit,
            PaginationService pagination) {
        this.conversions = conversions;
        this.howSolds = howSolds;
        this.products = products;
        this.audit = audit;
        this.pagination = pagination;
    }

    /** All active conversions, or only those assigned to one product (productId given). */
    @Transactional(readOnly = true)
    public List<HowSoldConversionDtos.Response> list(UUID productId) {
        List<HowSoldConversion> result = productId == null
                ? conversions.findAllByActiveTrue()
                : conversions.findAllByProductIdAndActiveTrue(productId);
        return result.stream().map(HowSoldConversionDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<HowSoldConversionDtos.Response> page(PageQuery query) {
        return pagination.page(conversions, query, SORT_FIELDS, SEARCH_FIELDS, HowSoldConversionDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public HowSoldConversionDtos.Response get(UUID id) {
        return HowSoldConversionDtos.Response.from(find(id));
    }

    @Transactional
    public HowSoldConversionDtos.Response create(HowSoldConversionDtos.Request request) {
        requireDistinct(request);
        HowSoldConversion conversion = new HowSoldConversion(
                findHowSold(request.parentHowSoldId()),
                findHowSold(request.childHowSoldId()),
                request.unitConversion());
        conversion.setProduct(findProduct(request.productId()));
        conversions.save(conversion);
        audit.record(AuditAction.CREATE, ENTITY, conversion.getId());
        return HowSoldConversionDtos.Response.from(conversion);
    }

    @Transactional
    public HowSoldConversionDtos.Response update(UUID id, HowSoldConversionDtos.Request request) {
        requireDistinct(request);
        HowSoldConversion conversion = find(id);
        // Keeping the units it already has is always fine, even if one was deleted since.
        if (!conversion.getParentHowSold().getId().equals(request.parentHowSoldId())) {
            conversion.setParentHowSold(findHowSold(request.parentHowSoldId()));
        }
        if (!conversion.getChildHowSold().getId().equals(request.childHowSoldId())) {
            conversion.setChildHowSold(findHowSold(request.childHowSoldId()));
        }
        conversion.setUnitConversion(request.unitConversion());
        conversion.setProduct(findProduct(request.productId()));
        // Flush so @PreUpdate has set updatedAt before the response is built.
        conversions.saveAndFlush(conversion);
        audit.record(AuditAction.UPDATE, ENTITY, conversion.getId());
        return HowSoldConversionDtos.Response.from(conversion);
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID id) {
        find(id).setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private void requireDistinct(HowSoldConversionDtos.Request request) {
        if (request.parentHowSoldId().equals(request.childHowSoldId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Parent and child how sold must be different");
        }
    }

    private HowSoldConversion find(UUID id) {
        return conversions.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversion not found"));
    }

    private HowSold findHowSold(UUID id) {
        return howSolds.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "How sold not found"));
    }

    /** Null means "all products". */
    private Product findProduct(UUID id) {
        if (id == null) {
            return null;
        }
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
