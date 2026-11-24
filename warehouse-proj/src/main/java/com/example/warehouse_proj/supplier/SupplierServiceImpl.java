package com.example.warehouse_proj.supplier;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import com.example.warehouse_proj.reorder.ReorderRepository;
import com.example.warehouse_proj.reorder.ReorderStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl implements SupplierService {

    private static final String ENTITY = "Supplier";
    private static final Set<String> SORT_FIELDS = Set.of("supplierName", "phoneNumber", "email");

    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final SupplierProductRepository links;
    private final AuditLogService audit;
    private final PaginationService pagination;
    private final ReorderRepository reorders;

    public SupplierServiceImpl(
            SupplierRepository suppliers,
            ProductRepository products,
            SupplierProductRepository links,
            AuditLogService audit,
            PaginationService pagination,
            ReorderRepository reorders) {
        this.suppliers = suppliers;
        this.products = products;
        this.links = links;
        this.audit = audit;
        this.pagination = pagination;
        this.reorders = reorders;
    }

    @Transactional(readOnly = true)
    @Override
    public List<SupplierDtos.Response> list() {
        return toResponses(suppliers.findAllByActiveTrue());
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<SupplierDtos.Response> page(PageQuery query) {
        Page<Supplier> result = suppliers.findAllByActiveTrue(pagination.toPageable(query, SORT_FIELDS));
        Map<UUID, Set<UUID>> productIds = productIdsBySupplier(result.getContent());
        return PageResponse.of(result, s -> SupplierDtos.Response.from(s, productIds.getOrDefault(s.getId(), Set.of())));
    }

    @Transactional(readOnly = true)
    @Override
    public SupplierDtos.Response get(UUID id) {
        return toResponses(List.of(find(id))).getFirst();
    }

    @Transactional
    @Override
    public SupplierDtos.Response create(SupplierDtos.Request request) {
        Supplier supplier = new Supplier(
                request.supplierName().trim(), request.phoneNumber().trim(), request.email().trim());
        suppliers.save(supplier);
        audit.record(AuditAction.CREATE, ENTITY, supplier.getId());
        return SupplierDtos.Response.from(supplier, Set.of());
    }

    @Transactional
    @Override
    public SupplierDtos.Response update(UUID id, SupplierDtos.Request request) {
        Supplier supplier = find(id);
        supplier.setSupplierName(request.supplierName().trim());
        supplier.setPhoneNumber(request.phoneNumber().trim());
        supplier.setEmail(request.email().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        suppliers.saveAndFlush(supplier);
        audit.record(AuditAction.UPDATE, ENTITY, supplier.getId());
        return toResponses(List.of(supplier)).getFirst();
    }

    /** Soft delete: the supplier and its product links stay in the table with isActive = false. */
    @Transactional
    @Override
    public void delete(UUID id) {
        Supplier supplier = find(id);
        if (reorders.existsBySupplierIdAndActiveTrueAndStatusIn(id, ReorderStatus.open())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Supplier still has open reorders");
        }
        links.findAllBySupplierIdAndActiveTrue(id).forEach(link -> link.setActive(false));
        supplier.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    /** Links the product, reactivating a previously removed link instead of inserting a duplicate. */
    @Transactional
    @Override
    public void addProduct(UUID supplierId, UUID productId) {
        Supplier supplier = find(supplierId);
        Product product = findProduct(productId);
        SupplierProduct link = links.findBySupplierIdAndProductId(supplierId, productId)
                .orElseGet(() -> new SupplierProduct(supplier, product));
        link.setActive(true);
        links.save(link);
        audit.record(AuditAction.UPDATE, ENTITY, supplierId);
    }

    /** Soft-deletes the link; a no-op if the product was not linked. */
    @Transactional
    @Override
    public void removeProduct(UUID supplierId, UUID productId) {
        find(supplierId);
        findProduct(productId);
        links.findBySupplierIdAndProductId(supplierId, productId).ifPresent(link -> link.setActive(false));
        audit.record(AuditAction.UPDATE, ENTITY, supplierId);
    }

    private List<SupplierDtos.Response> toResponses(Collection<Supplier> list) {
        Map<UUID, Set<UUID>> productIds = productIdsBySupplier(list);
        return list.stream()
                .map(s -> SupplierDtos.Response.from(s, productIds.getOrDefault(s.getId(), Set.of())))
                .toList();
    }

    /** Active products linked (via active links) to each supplier, in one query. */
    private Map<UUID, Set<UUID>> productIdsBySupplier(Collection<Supplier> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = list.stream().map(Supplier::getId).toList();
        return links.findAllBySupplierIdInAndActiveTrue(ids).stream()
                .filter(link -> link.getProduct().isActive())
                .collect(Collectors.groupingBy(
                        link -> link.getSupplier().getId(),
                        Collectors.mapping(link -> link.getProduct().getId(), Collectors.toSet())));
    }

    private Supplier find(UUID id) {
        return suppliers.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    private Product findProduct(UUID id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
