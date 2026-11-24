package com.example.warehouse_proj.reorder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ReorderDtos {

    private ReorderDtos() {
    }

    /**
     * expectedDate and howSoldId (the unit the quantity is counted in) are optional; status defaults to DRAFT on
     * create, unchanged on update.
     */
    public record Request(
            @NotNull UUID supplierId,
            @NotNull UUID productId,
            @NotNull @Min(1) @Max(1_000_000) Integer quantity,
            UUID howSoldId,
            LocalDate expectedDate,
            ReorderStatus status) {
    }

    public record Response(
            UUID id,
            UUID supplierId,
            UUID productId,
            int quantity,
            UUID howSoldId,
            String howSoldName,
            LocalDate expectedDate,
            ReorderStatus status,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(Reorder r) {
            return new Response(
                    r.getId(),
                    r.getSupplier().getId(),
                    r.getProduct().getId(),
                    r.getQuantity(),
                    r.getHowSold() == null ? null : r.getHowSold().getId(),
                    r.getHowSold() == null ? null : r.getHowSold().getName(),
                    r.getExpectedDate(),
                    r.getStatus(),
                    r.isActive(),
                    r.getCreatedAt(),
                    r.getUpdatedAt());
        }
    }
}
