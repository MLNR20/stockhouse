package com.example.warehouse_proj.seed;

import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldRepository;
import com.example.warehouse_proj.inventory.Inventory;
import com.example.warehouse_proj.inventory.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Random;

/**
 * Gives sample inventory records a quantity on hand, a minimum threshold and a "how sold" unit.
 * Only runs while every inventory record still has the empty defaults (0 / 0 / not set), so it never
 * overwrites values someone entered. Turn off with app.seed.enabled=false.
 */
@Component
@Order(4)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class InventoryDefaultsSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryDefaultsSeeder.class);

    private final TransactionTemplate tx;
    private final InventoryRepository inventories;
    private final HowSoldRepository howSolds;

    public InventoryDefaultsSeeder(TransactionTemplate tx, InventoryRepository inventories, HowSoldRepository howSolds) {
        this.tx = tx;
        this.inventories = inventories;
        this.howSolds = howSolds;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            List<Inventory> all = inventories.findAllByActiveTrue();
            boolean untouched = all.stream()
                    .allMatch(i -> i.getHowSold() == null && i.getBaseQuantity() == 0 && i.getMinimumThreshold() == 0);
            List<HowSold> units = howSolds.findAllByActiveTrue();
            if (all.isEmpty() || !untouched || units.isEmpty()) {
                return;
            }

            Random random = new Random(7); // fixed seed: the same data every time
            for (int i = 0; i < all.size(); i++) {
                Inventory inventory = all.get(i);
                int threshold = 20 + (i % 4) * 10;
                int quantity = switch (inventory.getStatus()) {
                    case IN_STOCK -> threshold + 30 + random.nextInt(300);
                    case LOW_STOCK -> Math.max(1, threshold - 5 - random.nextInt(10)); // below the threshold
                    case OUT_OF_STOCK -> 0;
                    case DISCONTINUED -> random.nextInt(10);
                };
                inventory.setMinimumThreshold(threshold);
                inventory.setBaseQuantity(quantity);
                inventory.setHowSold(units.get(i % units.size()));
            }
            log.info("Set sample quantities, thresholds and units on {} inventory records", all.size());
        });
    }
}
