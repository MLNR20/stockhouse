package com.example.warehouse_proj.analytics;

import com.example.warehouse_proj.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record Totals(
            long products, long suppliers, long customers, long categories, long warehouses, long containers) {
    }

    /**
     * @param open      orders still in progress (not delivered or cancelled)
     * @param overdue   open orders whose target delivery date has passed
     * @param revenue   total price of every order that is not cancelled
     * @param openValue total price of the open orders
     * @param byStatus  one entry per status, zeros included
     */
    public record OrdersSummary(
            long total,
            long open,
            long overdue,
            BigDecimal revenue,
            BigDecimal openValue,
            List<OrderStatusStat> byStatus) {
    }

    public record DeliveryDay(LocalDate date, long count) {
    }

    public record Summary(
            Totals totals,
            OrdersSummary orders,
            List<InventoryStatusStat> inventoryByStatus,
            List<CategoryStat> productsByCategory,
            List<DeliveryDay> deliveriesNext14Days) {
    }

    /** Statuses that count as "still open". */
    public static final List<OrderStatus> CLOSED_STATUSES = List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
}
