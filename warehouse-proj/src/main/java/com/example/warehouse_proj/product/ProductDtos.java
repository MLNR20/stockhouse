package com.example.warehouse_proj.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    /** A supplier linked to a product. Soft-deleted suppliers stay listed, with active = false. */
    public record SupplierRef(UUID id, String name, boolean active) {
    }

    public record Response(
            UUID id,
            String name,
            BigDecimal basePrice,
            UUID categoryId,
            String categoryName,
            /** False when the product's category was soft-deleted (it is still shown). True when there is none. */
            boolean categoryActive,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            List<UUID> supplierIds,
            List<SupplierRef> suppliers) {

        static Response from(Product p, List<SupplierRef> suppliers) {
            return new Response(
                    p.getId(),
                    p.getName(),
                    p.getBasePrice(),
                    p.getCategory() == null ? null : p.getCategory().getId(),
                    p.getCategory() == null ? null : p.getCategory().getName(),
                    p.getCategory() == null || p.getCategory().isActive(),
                    p.isActive(),
                    p.getCreatedAt(),
                    p.getUpdatedAt(),
                    suppliers.stream().map(SupplierRef::id).toList(),
                    suppliers);
        }
    }
}
