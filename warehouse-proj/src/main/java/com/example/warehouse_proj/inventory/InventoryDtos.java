package com.example.warehouse_proj.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    /**
     * baseQuantity and minimumThreshold are optional: 0 on create, unchanged on update when left out.
     * howSoldId is optional too; null means "not set" (on update it clears the unit).
     */
    public record Request(
            @NotNull UUID productId,
            @NotNull InventoryStatus status,
            @Min(0) @Max(1_000_000) Integer baseQuantity,
            @Min(0) @Max(1_000_000) Integer minimumThreshold,
            UUID howSoldId) {
    }

    /**
     * belowMinimum is true when the quantity on hand has fallen below the minimum threshold. A deleted unit is
     * still named, with howSoldActive = false (true when there is no unit).
     */
    public record Response(
            UUID id,
            UUID productId,
            InventoryStatus status,
            int baseQuantity,
            int minimumThreshold,
            boolean belowMinimum,
            UUID howSoldId,
            String howSoldName,
            boolean howSoldActive,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(Inventory i) {
            return new Response(
                    i.getId(),
                    i.getProduct().getId(),
                    i.getStatus(),
                    i.getBaseQuantity(),
                    i.getMinimumThreshold(),
                    i.getBaseQuantity() < i.getMinimumThreshold(),
                    i.getHowSold() == null ? null : i.getHowSold().getId(),
                    i.getHowSold() == null ? null : i.getHowSold().getName(),
                    i.getHowSold() == null || i.getHowSold().isActive(),
                    i.isActive(),
                    i.getCreatedAt(),
                    i.getUpdatedAt());
        }
    }
}
