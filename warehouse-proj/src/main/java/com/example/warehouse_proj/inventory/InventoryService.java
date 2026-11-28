package com.example.warehouse_proj.inventory;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldRepository;
import com.example.warehouse_proj.notification.NotificationService;
import com.example.warehouse_proj.notification.NotificationType;
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
public class InventoryService {

    private static final String ENTITY = "Inventory";
    private static final Set<String> SORT_FIELDS = Set.of("status", "baseQuantity", "minimumThreshold");
    private static final List<String> SEARCH_FIELDS = List.of(
            "product.name", "howSold.name", "status");

    private final InventoryRepository inventories;
    private final ProductRepository products;
    private final HowSoldRepository howSolds;
    private final AuditLogService audit;
    private final PaginationService pagination;
    private final NotificationService notifications;

    public InventoryService(
            InventoryRepository inventories,
            ProductRepository products,
            HowSoldRepository howSolds,
            AuditLogService audit,
            PaginationService pagination,
            NotificationService notifications) {
        this.inventories = inventories;
        this.products = products;
        this.howSolds = howSolds;
        this.audit = audit;
        this.pagination = pagination;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<InventoryDtos.Response> list(UUID productId) {
        List<Inventory> result = productId == null
                ? inventories.findAllByActiveTrue()
                : inventories.findAllByProductIdAndActiveTrue(productId);
        return result.stream().map(InventoryDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryDtos.Response> page(PageQuery query) {
        return pagination.page(inventories, query, SORT_FIELDS, SEARCH_FIELDS, InventoryDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public InventoryDtos.Response get(UUID id) {
        return InventoryDtos.Response.from(find(id));
    }

    @Transactional
    public InventoryDtos.Response create(InventoryDtos.Request request) {
        Inventory inventory = new Inventory(findProduct(request.productId()), request.status());
        inventory.setBaseQuantity(request.baseQuantity() != null ? request.baseQuantity() : 0);
        inventory.setMinimumThreshold(request.minimumThreshold() != null ? request.minimumThreshold() : 0);
        inventory.setHowSold(findHowSold(request.howSoldId()));
        inventories.save(inventory);
        audit.record(AuditAction.CREATE, ENTITY, inventory.getId());
        notifyIfStockAlert(inventory, null);
        return InventoryDtos.Response.from(inventory);
    }

    @Transactional
    public InventoryDtos.Response update(UUID id, InventoryDtos.Request request) {
        Inventory inventory = find(id);
        InventoryStatus previousStatus = inventory.getStatus();
        inventory.setProduct(findProduct(request.productId()));
        inventory.setStatus(request.status());
        if (request.baseQuantity() != null) {
            inventory.setBaseQuantity(request.baseQuantity());
        }
        if (request.minimumThreshold() != null) {
            inventory.setMinimumThreshold(request.minimumThreshold());
        }
        // Keeping the unit it already has is always fine, even if that unit was deleted since.
        HowSold unit = inventory.getHowSold();
        if (unit == null || !unit.getId().equals(request.howSoldId())) {
            inventory.setHowSold(findHowSold(request.howSoldId()));
        }
        // Flush so @PreUpdate has set updatedAt before the response is built.
        inventories.saveAndFlush(inventory);
        audit.record(AuditAction.UPDATE, ENTITY, inventory.getId());
        notifyIfStockAlert(inventory, previousStatus);
        return InventoryDtos.Response.from(inventory);
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID id) {
        find(id).setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    /** Tells every user when stock newly drops to LOW_STOCK or OUT_OF_STOCK (previous may be null). */
    private void notifyIfStockAlert(Inventory inventory, InventoryStatus previous) {
        InventoryStatus status = inventory.getStatus();
        if (status == previous) {
            return;
        }
        String productName = inventory.getProduct().getName();
        if (status == InventoryStatus.LOW_STOCK) {
            notifications.notifyAllUsers(
                    NotificationType.LOW_STOCK,
                    "Low stock",
                    productName + " is running low",
                    ENTITY,
                    inventory.getId());
        } else if (status == InventoryStatus.OUT_OF_STOCK) {
            notifications.notifyAllUsers(
                    NotificationType.OUT_OF_STOCK,
                    "Out of stock",
                    productName + " is out of stock",
                    ENTITY,
                    inventory.getId());
        }
    }

    private Inventory find(UUID id) {
        return inventories.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
    }

    private Product findProduct(UUID id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    /** Null means "not set". */
    private HowSold findHowSold(UUID id) {
        if (id == null) {
            return null;
        }
        return howSolds.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "How sold not found"));
    }
}
