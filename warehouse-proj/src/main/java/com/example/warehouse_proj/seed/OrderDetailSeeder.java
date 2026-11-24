package com.example.warehouse_proj.seed;

import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.order.OrderRepository;
import com.example.warehouse_proj.orderdetail.OrderDetail;
import com.example.warehouse_proj.orderdetail.OrderDetailRepository;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sample order lines, 1 to 3 per order. The last line of each order takes whatever is left of the
 * order's total, so the lines always add up to exactly the order's total price. Runs only when there
 * are no order lines yet. Turn off with app.seed.enabled=false.
 */
@Component
@Order(5)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class OrderDetailSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderDetailSeeder.class);

    private static final BigDecimal SHARE = new BigDecimal("0.20"); // each early line is about 20% of the total

    private final TransactionTemplate tx;
    private final OrderDetailRepository details;
    private final OrderRepository orders;
    private final ProductRepository products;

    public OrderDetailSeeder(
            TransactionTemplate tx,
            OrderDetailRepository details,
            OrderRepository orders,
            ProductRepository products) {
        this.tx = tx;
        this.details = details;
        this.orders = orders;
        this.products = products;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            if (details.count() > 0) {
                return;
            }
            List<CustomerOrder> orderList = orders.findAllByActiveTrue();
            List<Product> productList = products.findAllByActiveTrue().stream()
                    .sorted(Comparator.comparing(Product::getName))
                    .toList();
            if (orderList.isEmpty() || productList.size() < 3) {
                return;
            }

            List<OrderDetail> created = new ArrayList<>();
            for (int i = 0; i < orderList.size(); i++) {
                CustomerOrder order = orderList.get(i);
                BigDecimal total = order.getTotalPrice();
                int lines = 1 + (i % 3);
                BigDecimal remaining = total;
                for (int k = 0; k < lines; k++) {
                    Product product = productList.get((i * 3 + k) % productList.size());
                    if (k < lines - 1) {
                        int quantity = 1 + ((i + k) % 4);
                        BigDecimal unit = total.multiply(SHARE)
                                .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP)
                                .max(new BigDecimal("0.01"));
                        created.add(new OrderDetail(order, product, quantity, unit));
                        remaining = remaining.subtract(unit.multiply(BigDecimal.valueOf(quantity)));
                    } else {
                        created.add(new OrderDetail(order, product, 1, remaining.max(BigDecimal.ZERO)));
                    }
                }
            }
            details.saveAll(created);
            log.info("Seeded {} order lines for {} orders", created.size(), orderList.size());
        });
    }
}
