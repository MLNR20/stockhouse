package com.example.warehouse_proj.howsold;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class HowSoldConversionDtos {

    private HowSoldConversionDtos() {
    }

    /** productId is optional: null means the conversion applies to every product. */
    public record Request(
            @NotNull UUID parentHowSoldId,
            @NotNull UUID childHowSoldId,
            @NotNull @DecimalMin(value = "0.0000", inclusive = false) @Digits(integer = 15, fraction = 4)
            BigDecimal unitConversion,
            UUID productId) {
    }

    public record Response(
            UUID id,
            UUID parentHowSoldId,
            UUID childHowSoldId,
            BigDecimal unitConversion,
            UUID productId,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(HowSoldConversion c) {
            return new Response(
                    c.getId(),
                    c.getParentHowSold().getId(),
                    c.getChildHowSold().getId(),
                    c.getUnitConversion(),
                    c.getProduct() == null ? null : c.getProduct().getId(),
                    c.isActive(),
                    c.getCreatedAt(),
                    c.getUpdatedAt());
        }
    }
}
