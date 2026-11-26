package com.example.warehouse_proj.analytics;

import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldConversionRepository;
import com.example.warehouse_proj.howsold.UnitConversions;
import com.example.warehouse_proj.inventory.Inventory;
import com.example.warehouse_proj.inventory.InventoryRepository;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.order.OrderStatus;
import com.example.warehouse_proj.orderdetail.OrderDetail;
import com.example.warehouse_proj.orderdetail.OrderDetailRepository;
import com.example.warehouse_proj.reorder.Reorder;
import com.example.warehouse_proj.reorder.ReorderRepository;
import com.example.warehouse_proj.reorder.ReorderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Orders against inventory, day by day from today. The database keeps no stock history, so this looks forward: it
 * starts from the stock on hand now, takes away what open orders need on their delivery date, and adds what
 * ordered reorders are expected to bring. All quantities are in base units (e.g. Each).
 *
 * <ul>
 *   <li>Open orders are those not yet shipped: pending or confirmed. Overdue ones count on the first day.</li>
 *   <li>Only reorders with status "ordered" and an expected date add stock; overdue ones arrive on the first day.</li>
 * </ul>
 */
@Service
public class OrderInventoryTrendService {

    static final int MIN_DAYS = 7;
    static final int MAX_DAYS = 180;

    private static final Set<OrderStatus> OPEN = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

    private final InventoryRepository inventories;
    private final OrderDetailRepository orderDetails;
    private final ReorderRepository reorders;
    private final HowSoldConversionRepository conversions;
    private final Clock clock = Clock.systemDefaultZone();

    public OrderInventoryTrendService(
            InventoryRepository inventories,
            OrderDetailRepository orderDetails,
            ReorderRepository reorders,
            HowSoldConversionRepository conversions) {
        this.inventories = inventories;
        this.orderDetails = orderDetails;
        this.reorders = reorders;
        this.conversions = conversions;
    }

    /** The next {@code days} days, for one product or (productId null) all of them together. */
    @Transactional(readOnly = true)
    public AnalyticsDtos.Trend trend(int days, UUID productId) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "days must be between " + MIN_DAYS + " and " + MAX_DAYS);
        }
        LocalDate today = LocalDate.now(clock);
        UnitConversions units = new UnitConversions(conversions.findAllByActiveTrue());

        long stockNow = 0;
        for (Inventory inventory : inventories.findAllByActiveTrue()) {
            if (matches(productId, inventory.getProduct().getId())) {
                stockNow += inventory.getBaseQuantity();
            }
        }

        Map<LocalDate, Long> ordered = new HashMap<>();
        for (OrderDetail line : orderDetails.findAllByActiveTrue()) {
            CustomerOrder order = line.getCustomerOrder();
            UUID product = line.getProduct().getId();
            if (order.isActive() && OPEN.contains(order.getStatus()) && matches(productId, product)) {
                add(ordered, notBefore(order.getTargetDeliveryDate(), today),
                        inBaseUnits(units, product, line.getHowSold(), line.getQuantity()));
            }
        }

        Map<LocalDate, Long> incoming = new HashMap<>();
        for (Reorder reorder : reorders.findAllByActiveTrue()) {
            UUID product = reorder.getProduct().getId();
            if (reorder.getStatus() == ReorderStatus.ORDERED && reorder.getExpectedDate() != null
                    && matches(productId, product)) {
                add(incoming, notBefore(reorder.getExpectedDate(), today),
                        inBaseUnits(units, product, reorder.getHowSold(), reorder.getQuantity()));
            }
        }

        return aggregate(today, days, stockNow, ordered, incoming);
    }

    /** Pure day-by-day roll-up: running totals of demand and arrivals applied to today's stock. */
    static AnalyticsDtos.Trend aggregate(
            LocalDate from,
            int days,
            long stockNow,
            Map<LocalDate, Long> ordered,
            Map<LocalDate, Long> incoming) {
        List<AnalyticsDtos.TrendDay> result = new ArrayList<>(days);
        long orderedTotal = 0;
        long incomingTotal = 0;
        LocalDate firstShortage = null;
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            long out = ordered.getOrDefault(date, 0L);
            long in = incoming.getOrDefault(date, 0L);
            orderedTotal += out;
            incomingTotal += in;
            long stock = stockNow + incomingTotal - orderedTotal;
            if (stock < 0 && firstShortage == null) {
                firstShortage = date;
            }
            result.add(new AnalyticsDtos.TrendDay(date, out, orderedTotal, in, stock));
        }
        return new AnalyticsDtos.Trend(
                from, from.plusDays(days - 1L), stockNow, orderedTotal, incomingTotal, firstShortage, result);
    }

    private static boolean matches(UUID wanted, UUID actual) {
        return wanted == null || wanted.equals(actual);
    }

    private static LocalDate notBefore(LocalDate date, LocalDate earliest) {
        return date.isBefore(earliest) ? earliest : date;
    }

    private static void add(Map<LocalDate, Long> totals, LocalDate date, long quantity) {
        totals.merge(date, quantity, Long::sum);
    }

    private static long inBaseUnits(UnitConversions units, UUID productId, HowSold unit, int quantity) {
        if (unit == null) {
            return quantity;
        }
        return units.toBase(productId, unit).factor()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
