package com.example.warehouse_proj.analytics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    /** Everything the dashboard shows, in one call. */
    @GetMapping("/summary")
    public AnalyticsDtos.Summary summary() {
        return service.summary();
    }
}
