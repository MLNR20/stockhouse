package com.example.warehouse_proj.reorder;

import java.util.List;

public enum ReorderStatus {
    /** Being prepared; nothing has been sent to the supplier yet. */
    DRAFT,
    /** Sent to the supplier, stock is on its way. */
    ORDERED,
    /** The stock arrived. Final. */
    RECEIVED,
    /** Called off. Final. */
    CANCELLED;

    /** Reorders that are still in progress. */
    public static List<ReorderStatus> open() {
        return List.of(DRAFT, ORDERED);
    }

    public boolean isFinal() {
        return this == RECEIVED || this == CANCELLED;
    }
}
