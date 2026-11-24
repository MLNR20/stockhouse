package com.example.warehouse_proj.seed;

import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldConversion;
import com.example.warehouse_proj.howsold.HowSoldConversionRepository;
import com.example.warehouse_proj.howsold.HowSoldRepository;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Sample per-product conversions ("1 Case of Claw Hammer = 12 Each"). Runs only while no conversion is
 * assigned to a product yet, and only if the Case and Each units exist. Turn off with app.seed.enabled=false.
 */
@Component
@Order(6)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class ConversionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConversionSeeder.class);

    /** Each per case, in turn, for the first products by name. */
    private static final int[] EACH_PER_CASE = {12, 24, 6, 10, 48, 36};

    private final TransactionTemplate tx;
    private final HowSoldConversionRepository conversions;
    private final HowSoldRepository howSolds;
    private final ProductRepository products;

    public ConversionSeeder(
            TransactionTemplate tx,
            HowSoldConversionRepository conversions,
            HowSoldRepository howSolds,
            ProductRepository products) {
        this.tx = tx;
        this.conversions = conversions;
        this.howSolds = howSolds;
        this.products = products;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            boolean anyAssigned = conversions.findAllByActiveTrue().stream().anyMatch(c -> c.getProduct() != null);
            if (anyAssigned) {
                return;
            }
            List<HowSold> units = howSolds.findAllByActiveTrue();
            Optional<HowSold> cas = byName(units, "Case");
            Optional<HowSold> each = byName(units, "Each");
            if (cas.isEmpty() || each.isEmpty()) {
                return;
            }
            List<Product> sample = products.findAllByActiveTrue().stream()
                    .sorted(Comparator.comparing(Product::getName))
                    .limit(EACH_PER_CASE.length)
                    .toList();
            for (int i = 0; i < sample.size(); i++) {
                HowSoldConversion conversion =
                        new HowSoldConversion(cas.get(), each.get(), BigDecimal.valueOf(EACH_PER_CASE[i]));
                conversion.setProduct(sample.get(i));
                conversions.save(conversion);
            }
            log.info("Seeded {} per-product unit conversions", sample.size());
        });
    }

    private static Optional<HowSold> byName(List<HowSold> units, String name) {
        return units.stream().filter(u -> u.getName().equalsIgnoreCase(name)).findFirst();
    }
}
