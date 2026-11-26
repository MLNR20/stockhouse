package com.example.warehouse_proj.container;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import com.example.warehouse_proj.shelfrow.ShelfRow;
import com.example.warehouse_proj.shelfrow.ShelfRowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ContainerService {

    private static final String ENTITY = "Container";
    private static final Set<String> SORT_FIELDS = Set.of("name");
    private static final List<String> SEARCH_FIELDS = List.of(
            "name", "shelfRow.name", "shelfRow.shelf.name");

    private final ContainerRepository containers;
    private final ContainerProductRepository contents;
    private final ShelfRowRepository rows;
    private final ProductRepository products;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public ContainerService(
            ContainerRepository containers,
            ContainerProductRepository contents,
            ShelfRowRepository rows,
            ProductRepository products,
            AuditLogService audit,
            PaginationService pagination) {
        this.containers = containers;
        this.contents = contents;
        this.rows = rows;
        this.products = products;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<ContainerDtos.Response> list(UUID rowId) {
        List<Container> result = rowId == null
                ? containers.findAllByActiveTrue()
                : containers.findAllByShelfRowIdAndActiveTrue(rowId);
        return result.stream().map(ContainerDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ContainerDtos.Response> page(PageQuery query) {
        return pagination.page(containers, query, SORT_FIELDS, SEARCH_FIELDS, ContainerDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public ContainerDtos.Response get(UUID id) {
        return ContainerDtos.Response.from(find(id));
    }

    @Transactional
    public ContainerDtos.Response create(ContainerDtos.Request request) {
        Container container = new Container(findRow(request.rowId()), request.name().trim());
        containers.save(container);
        audit.record(AuditAction.CREATE, ENTITY, container.getId());
        return ContainerDtos.Response.from(container);
    }

    @Transactional
    public ContainerDtos.Response update(UUID id, ContainerDtos.Request request) {
        Container container = find(id);
        container.setShelfRow(findRow(request.rowId()));
        container.setName(request.name().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        containers.saveAndFlush(container);
        audit.record(AuditAction.UPDATE, ENTITY, container.getId());
        return ContainerDtos.Response.from(container);
    }

    /** Soft delete; refused while the container still holds products. */
    @Transactional
    public void delete(UUID id) {
        Container container = find(id);
        if (contents.existsByContainerIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Container still holds products");
        }
        container.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    @Transactional(readOnly = true)
    public List<ContainerDtos.ContentResponse> listContents(UUID containerId) {
        find(containerId);
        return contents.findAllByContainerIdAndActiveTrue(containerId).stream()
                .map(ContainerDtos.ContentResponse::from)
                .toList();
    }

    /** Puts a product in the container or changes its quantity, reactivating a removed entry. */
    @Transactional
    public ContainerDtos.ContentResponse putContent(UUID containerId, UUID productId, int quantity) {
        Container container = find(containerId);
        Product product = findProduct(productId);
        ContainerProduct entry = contents.findByContainerIdAndProductId(containerId, productId)
                .orElseGet(() -> new ContainerProduct(container, product, quantity));
        entry.setActive(true);
        entry.setQuantity(quantity);
        // Flush so @PrePersist/@PreUpdate have set the timestamps before the response is built.
        contents.saveAndFlush(entry);
        audit.record(AuditAction.UPDATE, ENTITY, containerId);
        return ContainerDtos.ContentResponse.from(entry);
    }

    /** Soft-deletes the entry; a no-op if the product was not in the container. */
    @Transactional
    public void removeContent(UUID containerId, UUID productId) {
        find(containerId);
        contents.findByContainerIdAndProductId(containerId, productId).ifPresent(entry -> entry.setActive(false));
        audit.record(AuditAction.UPDATE, ENTITY, containerId);
    }

    private Container find(UUID id) {
        return containers.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Container not found"));
    }

    private ShelfRow findRow(UUID id) {
        return rows.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Row not found"));
    }

    private Product findProduct(UUID id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
