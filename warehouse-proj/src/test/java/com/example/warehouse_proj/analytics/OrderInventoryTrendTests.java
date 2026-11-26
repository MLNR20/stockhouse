package com.example.warehouse_proj.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderInventoryTrendTests {

    private final LocalDate today = LocalDate.of(2026, 3, 1);

    @Test
    void rollsDemandAndArrivalsIntoProjectedStock() {
        AnalyticsDtos.Trend trend = OrderInventoryTrendService.aggregate(
                today, 7, 100,
                Map.of(today, 30L, today.plusDays(2), 90L),
                Map.of(today.plusDays(4), 50L));

        assertEquals(7, trend.days().size());
        assertEquals(today.plusDays(6), trend.to());
        assertEquals(70, trend.days().get(0).stock());
        assertEquals(70, trend.days().get(1).stock(), "a quiet day keeps the level");
        assertEquals(-20, trend.days().get(2).stock());
        assertEquals(120, trend.days().get(2).orderedTotal());
        assertEquals(30, trend.days().get(4).stock(), "arrivals restore stock");
        assertEquals(120, trend.totalOrdered());
        assertEquals(50, trend.totalIncoming());
        assertEquals(today.plusDays(2), trend.firstShortage());
    }

    @Test
    void ignoresDatesOutsideTheRangeAndReportsNoShortage() {
        AnalyticsDtos.Trend trend = OrderInventoryTrendService.aggregate(
                today, 7, 10, Map.of(today.plusDays(30), 500L), Map.of());

        assertEquals(0, trend.totalOrdered());
        assertEquals(10, trend.days().get(6).stock());
        assertNull(trend.firstShortage());
    }
}
