package com.example.warehouse_proj.analytics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    private final OrderInventoryTrendService trends;

    public AnalyticsController(AnalyticsService service, OrderInventoryTrendService trends) {
        this.service = service;
        this.trends = trends;
    }

    /** Orders against inventory by day: GET /api/analytics/orders-vs-inventory?days=30&productId=... */
    @GetMapping("/orders-vs-inventory")
    public AnalyticsDtos.Trend ordersVsInventory(
            @RequestParam(defaultValue = "30") int days, @RequestParam(required = false) UUID productId) {
        return trends.trend(days, productId);
    }

    /** Everything the dashboard shows, in one call. */
    @GetMapping("/summary")
    public AnalyticsDtos.Summary summary() {
        return service.summary();
    }
}
