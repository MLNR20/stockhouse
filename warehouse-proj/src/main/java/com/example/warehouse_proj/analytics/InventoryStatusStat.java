package com.example.warehouse_proj.analytics;

import com.example.warehouse_proj.inventory.InventoryStatus;

/** Inventory records in one status. Built by a JPQL constructor expression. */
public record InventoryStatusStat(InventoryStatus status, Long count) {
}
