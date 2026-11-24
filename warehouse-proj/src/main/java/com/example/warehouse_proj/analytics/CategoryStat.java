package com.example.warehouse_proj.analytics;

/** Products in one category (category is null for uncategorised products). */
public record CategoryStat(String category, Long count) {
}
