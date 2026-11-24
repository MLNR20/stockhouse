package com.example.warehouse_proj.seed;

import com.example.warehouse_proj.inventory.Inventory;
import com.example.warehouse_proj.inventory.InventoryRepository;
import com.example.warehouse_proj.inventory.InventoryStatus;
import com.example.warehouse_proj.reorder.Reorder;
import com.example.warehouse_proj.reorder.ReorderRepository;
import com.example.warehouse_proj.reorder.ReorderStatus;
import com.example.warehouse_proj.supplier.SupplierProduct;
import com.example.warehouse_proj.supplier.SupplierProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sample supplier reorders: open ones for products that are running low or out, plus one received
 * and one cancelled. Runs after the other seeders and only when there are no reorders yet. Turn off
 * with app.seed.enabled=false. Written straight through the repositories, so receiving effects
 * (restocking, notifications) are not triggered by the seed itself.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class ReorderSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReorderSeeder.class);

    private final TransactionTemplate tx;
    private final ReorderRepository reorders;
    private final InventoryRepository inventories;
    private final SupplierProductRepository supplierProducts;

    public ReorderSeeder(
            TransactionTemplate tx,
            ReorderRepository reorders,
            InventoryRepository inventories,
            SupplierProductRepository supplierProducts) {
        this.tx = tx;
        this.reorders = reorders;
        this.inventories = inventories;
        this.supplierProducts = supplierProducts;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            if (reorders.count() > 0) {
                log.info("Reorder seed skipped: the database already has reorders");
                return;
            }
            List<Inventory> stock = inventories.findAllByActiveTrue();
            List<Reorder> created = new ArrayList<>();
            LocalDate today = LocalDate.now();

            // Open reorders for products that are low or out of stock.
            int n = 0;
            for (Inventory inventory : stock) {
                InventoryStatus s = inventory.getStatus();
                if (s != InventoryStatus.LOW_STOCK && s != InventoryStatus.OUT_OF_STOCK) {
                    continue;
                }
                Optional<SupplierProduct> link = firstLink(inventory);
                if (link.isEmpty()) {
                    continue;
                }
                boolean ordered = n % 2 == 0;
                created.add(new Reorder(
                        link.get().getSupplier(),
                        inventory.getProduct(),
                        s == InventoryStatus.OUT_OF_STOCK ? 200 : 120,
                        today.plusDays(4L + n * 3L),
                        ordered ? ReorderStatus.ORDERED : ReorderStatus.DRAFT));
                n++;
            }

            // One history entry each for a received and a cancelled reorder, for products in stock.
            boolean received = false;
            boolean cancelled = false;
            for (Inventory inventory : stock) {
                if (inventory.getStatus() != InventoryStatus.IN_STOCK || (received && cancelled)) {
                    continue;
                }
                Optional<SupplierProduct> link = firstLink(inventory);
                if (link.isEmpty()) {
                    continue;
                }
                ReorderStatus status2 = !received ? ReorderStatus.RECEIVED : ReorderStatus.CANCELLED;
                created.add(new Reorder(
                        link.get().getSupplier(),
                        inventory.getProduct(),
                        60,
                        today.minusDays(received ? 3 : 8),
                        status2));
                if (!received) {
                    received = true;
                } else {
                    cancelled = true;
                }
            }

            reorders.saveAll(created);
            log.info("Seeded {} reorders", created.size());
        });
    }

    private Optional<SupplierProduct> firstLink(Inventory inventory) {
        return supplierProducts.findAllByProductIdAndActiveTrue(inventory.getProduct().getId()).stream().findFirst();
    }
}
