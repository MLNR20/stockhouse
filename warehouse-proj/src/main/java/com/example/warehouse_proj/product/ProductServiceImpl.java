package com.example.warehouse_proj.product;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.category.Category;
import com.example.warehouse_proj.category.CategoryRepository;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.howsold.HowSoldConversionRepository;
import com.example.warehouse_proj.reorder.ReorderRepository;
import com.example.warehouse_proj.reorder.ReorderStatus;
import com.example.warehouse_proj.supplier.SupplierProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String ENTITY = "Product";
    private static final Set<String> SORT_FIELDS = Set.of("name", "basePrice");

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final SupplierProductRepository supplierLinks;
    private final AuditLogService audit;
    private final PaginationService pagination;
    private final ReorderRepository reorders;
    private final HowSoldConversionRepository conversions;

    public ProductServiceImpl(
            ProductRepository products,
            CategoryRepository categories,
            SupplierProductRepository supplierLinks,
            AuditLogService audit,
            PaginationService pagination,
            ReorderRepository reorders,
            HowSoldConversionRepository conversions) {
        this.products = products;
        this.categories = categories;
        this.supplierLinks = supplierLinks;
        this.audit = audit;
        this.pagination = pagination;
        this.reorders = reorders;
        this.conversions = conversions;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductDtos.Response> list(UUID categoryId) {
        return toResponses(categoryId == null
                ? products.findAllByActiveTrue()
                : products.findAllByCategoryIdAndActiveTrue(categoryId));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ProductDtos.Response> page(PageQuery query) {
        Page<Product> result = products.findAllByActiveTrue(pagination.toPageable(query, SORT_FIELDS));
        Map<UUID, List<ProductDtos.SupplierRef>> suppliers = suppliersByProduct(result.getContent());
        return PageResponse.of(result, p -> ProductDtos.Response.from(p, suppliers.getOrDefault(p.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    @Override
    public ProductDtos.Response get(UUID id) {
        return toResponses(List.of(find(id))).getFirst();
    }

    @Transactional
    @Override
    public ProductDtos.Response create(ProductDtos.Request request) {
        Product product = new Product(request.name().trim(), request.basePrice());
        product.setCategory(findCategory(request.categoryId()));
        products.save(product);
        audit.record(AuditAction.CREATE, ENTITY, product.getId());
        return ProductDtos.Response.from(product, List.of());
    }

    @Transactional
    @Override
    public ProductDtos.Response update(UUID id, ProductDtos.Request request) {
        Product product = find(id);
        product.setName(request.name().trim());
        product.setBasePrice(request.basePrice());
        // Keeping the category it already has is always fine, even if that category was deleted since.
        Category current = product.getCategory();
        if (current == null || request.categoryId() == null || !current.getId().equals(request.categoryId())) {
            product.setCategory(findCategory(request.categoryId()));
        }
        // Flush so @PreUpdate has set updatedAt before the response is built.
        products.saveAndFlush(product);
        audit.record(AuditAction.UPDATE, ENTITY, product.getId());
        return toResponses(List.of(product)).getFirst();
    }

    /** Soft delete: the product and its supplier links stay in the table with isActive = false. */
    @Transactional
    @Override
    public void delete(UUID id) {
        Product product = find(id);
        if (reorders.existsByProductIdAndActiveTrueAndStatusIn(id, ReorderStatus.open())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product still has open reorders");
        }
        supplierLinks.findAllByProductIdAndActiveTrue(id).forEach(link -> link.setActive(false));
        // Conversions assigned to this product mean nothing without it.
        conversions.findAllByProductIdAndActiveTrue(id).forEach(conversion -> conversion.setActive(false));
        product.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private List<ProductDtos.Response> toResponses(Collection<Product> list) {
        Map<UUID, List<ProductDtos.SupplierRef>> suppliers = suppliersByProduct(list);
        return list.stream()
                .map(p -> ProductDtos.Response.from(p, suppliers.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    /**
     * Suppliers linked (via active links) to each product, by name, in one query. A supplier that was soft-deleted
     * stays in the list (active = false) so the product still shows where it came from.
     */
    private Map<UUID, List<ProductDtos.SupplierRef>> suppliersByProduct(Collection<Product> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = list.stream().map(Product::getId).toList();
        return supplierLinks.findAllByProductIdInAndActiveTrue(ids).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getProduct().getId(),
                        Collectors.mapping(
                                link -> new ProductDtos.SupplierRef(
                                        link.getSupplier().getId(),
                                        link.getSupplier().getSupplierName(),
                                        link.getSupplier().isActive()),
                                Collectors.collectingAndThen(Collectors.toList(), refs -> refs.stream()
                                        .sorted(Comparator.comparing(
                                                ProductDtos.SupplierRef::name, String.CASE_INSENSITIVE_ORDER))
                                        .toList()))));
    }

    /** Null means "no category"; an id must belong to an active category. */
    private Category findCategory(UUID id) {
        if (id == null) {
            return null;
        }
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        if (!category.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That category has been deleted");
        }
        return category;
    }

    private Product find(UUID id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
