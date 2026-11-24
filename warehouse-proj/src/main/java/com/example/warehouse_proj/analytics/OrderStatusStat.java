package com.example.warehouse_proj.analytics;

import com.example.warehouse_proj.order.OrderStatus;

import java.math.BigDecimal;

/** Orders in one status: how many and their combined total price. Built by a JPQL constructor expression. */
public record OrderStatusStat(OrderStatus status, Long count, BigDecimal revenue) {
}
