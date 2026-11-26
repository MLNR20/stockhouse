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

    /** One day of the orders-against-inventory trend; quantities in base units. */
    public record TrendDay(
            java.time.LocalDate date,
            /** Units open orders need that day. */
            long ordered,
            /** Units needed from the first day up to and including this one. */
            long orderedTotal,
            /** Units reorders are expected to bring that day. */
            long incoming,
            /** Projected stock at the end of the day; negative means there is not enough. */
            long stock) {
    }

    public record Trend(
            java.time.LocalDate from,
            java.time.LocalDate to,
            long stockNow,
            long totalOrdered,
            long totalIncoming,
            /** The first day stock is projected to run short, or null. */
            java.time.LocalDate firstShortage,
            java.util.List<TrendDay> days) {
    }
}
