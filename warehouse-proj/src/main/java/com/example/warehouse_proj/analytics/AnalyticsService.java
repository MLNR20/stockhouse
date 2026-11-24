package com.example.warehouse_proj.analytics;

import com.example.warehouse_proj.analytics.AnalyticsDtos.DeliveryDay;
import com.example.warehouse_proj.analytics.AnalyticsDtos.OrdersSummary;
import com.example.warehouse_proj.analytics.AnalyticsDtos.Summary;
import com.example.warehouse_proj.analytics.AnalyticsDtos.Totals;
import com.example.warehouse_proj.category.CategoryRepository;
import com.example.warehouse_proj.container.ContainerRepository;
import com.example.warehouse_proj.customer.CustomerRepository;
import com.example.warehouse_proj.inventory.InventoryRepository;
import com.example.warehouse_proj.inventory.InventoryStatus;
import com.example.warehouse_proj.order.OrderRepository;
import com.example.warehouse_proj.order.OrderStatus;
import com.example.warehouse_proj.product.ProductRepository;
import com.example.warehouse_proj.supplier.SupplierRepository;
import com.example.warehouse_proj.warehouse.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read-only dashboard numbers, computed in the database (grouped queries, not by loading every row). */
@Service
public class AnalyticsService {

    private static final int DELIVERY_DAYS = 14;
    private static final String UNCATEGORISED = "Uncategorised";

    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final CustomerRepository customers;
    private final CategoryRepository categories;
    private final WarehouseRepository warehouses;
    private final ContainerRepository containers;
    private final OrderRepository orders;
    private final InventoryRepository inventories;

    public AnalyticsService(
            ProductRepository products,
            SupplierRepository suppliers,
            CustomerRepository customers,
            CategoryRepository categories,
            WarehouseRepository warehouses,
            ContainerRepository containers,
            OrderRepository orders,
            InventoryRepository inventories) {
        this.products = products;
        this.suppliers = suppliers;
        this.customers = customers;
        this.categories = categories;
        this.warehouses = warehouses;
        this.containers = containers;
        this.orders = orders;
        this.inventories = inventories;
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        LocalDate today = LocalDate.now();

        Totals totals = new Totals(
                products.countByActiveTrue(),
                suppliers.countByActiveTrue(),
                customers.countByActiveTrue(),
                categories.countByActiveTrue(),
                warehouses.countByActiveTrue(),
                containers.countByActiveTrue());

        return new Summary(
                totals,
                ordersSummary(today),
                inventoryByStatus(),
                productsByCategory(),
                deliveries(today));
    }

    private OrdersSummary ordersSummary(LocalDate today) {
        Map<OrderStatus, OrderStatusStat> found =
                orders.statusStats().stream().collect(Collectors.toMap(OrderStatusStat::status, Function.identity()));

        // One entry per status, in enum order, so the client never has to fill in zeros.
        List<OrderStatusStat> byStatus = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.add(found.getOrDefault(status, new OrderStatusStat(status, 0L, BigDecimal.ZERO)));
        }

        long total = 0;
        long open = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal openValue = BigDecimal.ZERO;
        for (OrderStatusStat stat : byStatus) {
            total += stat.count();
            if (stat.status() != OrderStatus.CANCELLED) {
                revenue = revenue.add(stat.revenue());
            }
            if (!AnalyticsDtos.CLOSED_STATUSES.contains(stat.status())) {
                open += stat.count();
                openValue = openValue.add(stat.revenue());
            }
        }

        long overdue = orders.countByActiveTrueAndTargetDeliveryDateBeforeAndStatusNotIn(
                today, AnalyticsDtos.CLOSED_STATUSES);
        return new OrdersSummary(total, open, overdue, revenue, openValue, byStatus);
    }

    private List<InventoryStatusStat> inventoryByStatus() {
        Map<InventoryStatus, Long> found = inventories.statusStats().stream()
                .collect(Collectors.toMap(InventoryStatusStat::status, InventoryStatusStat::count));
        Map<InventoryStatus, Long> all = new EnumMap<>(InventoryStatus.class);
        for (InventoryStatus status : InventoryStatus.values()) {
            all.put(status, found.getOrDefault(status, 0L));
        }
        return all.entrySet().stream().map(e -> new InventoryStatusStat(e.getKey(), e.getValue())).toList();
    }

    private List<CategoryStat> productsByCategory() {
        return products.countByCategory().stream()
                .map(stat -> new CategoryStat(stat.category() == null ? UNCATEGORISED : stat.category(), stat.count()))
                .toList();
    }

    /** Open orders due on each of the next DELIVERY_DAYS days (today included), zeros filled in. */
    private List<DeliveryDay> deliveries(LocalDate today) {
        Map<LocalDate, Long> found = orders
                .openDeliveriesBetween(today, today.plusDays(DELIVERY_DAYS - 1), AnalyticsDtos.CLOSED_STATUSES)
                .stream()
                .collect(Collectors.toMap(DayCount::date, DayCount::count));
        List<DeliveryDay> days = new ArrayList<>();
        for (int i = 0; i < DELIVERY_DAYS; i++) {
            LocalDate date = today.plusDays(i);
            days.add(new DeliveryDay(date, found.getOrDefault(date, 0L)));
        }
        return days;
    }
}
