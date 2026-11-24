package com.example.warehouse_proj.analytics;

import java.time.LocalDate;

/** Open orders due on one day. */
public record DayCount(LocalDate date, Long count) {
}
