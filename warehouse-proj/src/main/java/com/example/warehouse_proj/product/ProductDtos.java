package com.example.warehouse_proj.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class ProductDtos {

    private ProductDtos() {
    }

    /** categoryId is optional; null leaves the product uncategorised. */
    public record Request(
            @NotBlank @Size(max = 200) String name,
            @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal basePrice,
            UUID categoryId) {
    }

    public record Response(
            UUID id,
            String name,
            BigDecimal basePrice,
            UUID categoryId,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            Set<UUID> supplierIds) {

        static Response from(Product p, Set<UUID> supplierIds) {
            return new Response(
                    p.getId(),
                    p.getName(),
                    p.getBasePrice(),
                    p.getCategory() == null ? null : p.getCategory().getId(),
                    p.isActive(),
                    p.getCreatedAt(),
                    p.getUpdatedAt(),
                    supplierIds);
        }
    }
}
