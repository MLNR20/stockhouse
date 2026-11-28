package com.example.warehouse_proj.reorder;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldRepository;
import com.example.warehouse_proj.inventory.Inventory;
import com.example.warehouse_proj.inventory.InventoryRepository;
import com.example.warehouse_proj.inventory.InventoryStatus;
import com.example.warehouse_proj.notification.NotificationService;
import com.example.warehouse_proj.notification.NotificationType;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import com.example.warehouse_proj.supplier.Supplier;
import com.example.warehouse_proj.supplier.SupplierProductRepository;
import com.example.warehouse_proj.supplier.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ReorderService {

    private static final String ENTITY = "Reorder";
    private static final Set<String> SORT_FIELDS = Set.of("quantity", "expectedDate", "status");
    private static final List<String> SEARCH_FIELDS = List.of(
            "product.name", "supplier.supplierName", "status");

    private final ReorderRepository reorders;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final SupplierProductRepository supplierProducts;
    private final InventoryRepository inventories;
    private final HowSoldRepository howSolds;
    private final AuditLogService audit;
    private final PaginationService pagination;
    private final NotificationService notifications;

    public ReorderService(
            ReorderRepository reorders,
            SupplierRepository suppliers,
            ProductRepository products,
            SupplierProductRepository supplierProducts,
            InventoryRepository inventories,
            HowSoldRepository howSolds,
            AuditLogService audit,
            PaginationService pagination,
            NotificationService notifications) {
        this.howSolds = howSolds;
        this.reorders = reorders;
        this.suppliers = suppliers;
        this.products = products;
        this.supplierProducts = supplierProducts;
        this.inventories = inventories;
        this.audit = audit;
        this.pagination = pagination;
        this.notifications = notifications;
    }

    /** All active reorders, optionally only those for one supplier and/or one product. */
    @Transactional(readOnly = true)
    public List<ReorderDtos.Response> list(UUID supplierId, UUID productId) {
        List<Reorder> result;
        if (supplierId != null) {
            result = reorders.findAllBySupplierIdAndActiveTrue(supplierId);
        } else if (productId != null) {
            result = reorders.findAllByProductIdAndActiveTrue(productId);
        } else {
            result = reorders.findAllByActiveTrue();
        }
        return result.stream()
                .filter(r -> productId == null || r.getProduct().getId().equals(productId))
                .map(ReorderDtos.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReorderDtos.Response> page(PageQuery query) {
        return pagination.page(reorders, query, SORT_FIELDS, SEARCH_FIELDS, ReorderDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public ReorderDtos.Response get(UUID id) {
        return ReorderDtos.Response.from(find(id));
    }

    @Transactional
    public ReorderDtos.Response create(ReorderDtos.Request request) {
        ReorderStatus status = request.status() != null ? request.status() : ReorderStatus.DRAFT;
        if (status == ReorderStatus.RECEIVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A new reorder cannot already be received; mark it received once the stock arrives");
        }
        Supplier supplier = findSupplier(request.supplierId());
        Product product = findProduct(request.productId());
        requireSupplies(supplier.getId(), product.getId());

        Reorder reorder = new Reorder(supplier, product, request.quantity(), request.expectedDate(), status);
        reorder.setHowSold(findHowSold(request.howSoldId()));
        reorders.save(reorder);
        audit.record(AuditAction.CREATE, ENTITY, reorder.getId());
        return ReorderDtos.Response.from(reorder);
    }

    @Transactional
    public ReorderDtos.Response update(UUID id, ReorderDtos.Request request) {
        Reorder reorder = find(id);
        if (reorder.getStatus().isFinal()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A " + reorder.getStatus().name().toLowerCase() + " reorder can no longer be changed");
        }

        if (!reorder.getSupplier().getId().equals(request.supplierId())) {
            reorder.setSupplier(findSupplier(request.supplierId()));
        }
        if (!reorder.getProduct().getId().equals(request.productId())) {
            reorder.setProduct(findProduct(request.productId()));
        }
        requireSupplies(reorder.getSupplier().getId(), reorder.getProduct().getId());
        reorder.setQuantity(request.quantity());
        // Keeping the unit it already has is always fine, even if that unit was deleted since.
        HowSold unit = reorder.getHowSold();
        if (unit == null || !unit.getId().equals(request.howSoldId())) {
            reorder.setHowSold(findHowSold(request.howSoldId()));
        }
        reorder.setExpectedDate(request.expectedDate());

        ReorderStatus next = request.status() != null ? request.status() : reorder.getStatus();
        reorder.setStatus(next);
        // Flush so @PreUpdate has set updatedAt before the response is built.
        reorders.saveAndFlush(reorder);
        audit.record(AuditAction.UPDATE, ENTITY, reorder.getId());

        if (next == ReorderStatus.RECEIVED) {
            onReceived(reorder);
        }
        return ReorderDtos.Response.from(reorder);
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID id) {
        find(id).setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    /**
     * The stock arrived: any low or out-of-stock inventory record of that product goes back to
     * IN_STOCK, and everyone is told.
     */
    private void onReceived(Reorder reorder) {
        for (Inventory inventory : inventories.findAllByProductIdAndActiveTrue(reorder.getProduct().getId())) {
            if (inventory.getStatus() == InventoryStatus.LOW_STOCK
                    || inventory.getStatus() == InventoryStatus.OUT_OF_STOCK) {
                inventory.setStatus(InventoryStatus.IN_STOCK);
                audit.record(AuditAction.UPDATE, "Inventory", inventory.getId());
            }
        }
        notifications.notifyAllUsers(
                NotificationType.REORDER_RECEIVED,
                "Reorder received",
                reorder.getQuantity() + " × "
                        + (reorder.getHowSold() == null ? "" : reorder.getHowSold().getName() + " of ")
                        + reorder.getProduct().getName() + " received from "
                        + reorder.getSupplier().getSupplierName(),
                ENTITY,
                reorder.getId());
    }

    /** A supplier can only be asked for products it supplies (an active supplier-product link). */
    private void requireSupplies(UUID supplierId, UUID productId) {
        boolean supplies = supplierProducts.findBySupplierIdAndProductId(supplierId, productId)
                .filter(BaseEntity::isActive)
                .isPresent();
        if (!supplies) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That supplier does not supply this product");
        }
    }

    private Reorder find(UUID id) {
        return reorders.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reorder not found"));
    }

    private Supplier findSupplier(UUID id) {
        return suppliers.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    /** The unit is optional: no id means "not set". */
    private HowSold findHowSold(UUID id) {
        if (id == null) {
            return null;
        }
        return howSolds.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit of measure not found"));
    }

    private Product findProduct(UUID id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
