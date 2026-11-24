package com.example.warehouse_proj.orderdetail;

import com.example.warehouse_proj.howsold.UnitConversions;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public final class OrderDetailDtos {

    private OrderDetailDtos() {
    }

    /**
     * salesPrice is optional: when left out the product's base price is used.
     * howSoldId is optional: when left out the unit of the product's inventory record is used (if it has one).
     */
    public record Request(
            @NotNull UUID productId,
            @NotNull @Min(1) @Max(1_000_000) Integer quantity,
            @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal salesPrice,
            UUID howSoldId) {
    }

    /**
     * @param howSoldId          the unit the quantity is counted in (null = the base unit)
     * @param basePrice          the product's current base price, to compare with salesPrice
     * @param baseUnitName       the base unit the stock is counted in (e.g. "Each")
     * @param baseQuantityNeeded the line's quantity converted to base units (2 Case of 12 Each = 24)
     * @param availableQuantity  base quantity on hand for the product (all its active inventory records)
     * @param inStock            whether availableQuantity covers baseQuantityNeeded
     */
    public record Response(
            UUID id,
            UUID orderId,
            UUID productId,
            String productName,
            UUID howSoldId,
            String howSoldName,
            int quantity,
            BigDecimal salesPrice,
            BigDecimal lineTotal,
            BigDecimal basePrice,
            String baseUnitName,
            long baseQuantityNeeded,
            long availableQuantity,
            boolean inStock,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(OrderDetail d, long availableQuantity, UnitConversions.Resolved base) {
            long needed = BigDecimal.valueOf(d.getQuantity())
                    .multiply(base.factor())
                    .setScale(0, RoundingMode.CEILING)
                    .longValue();
            return new Response(
                    d.getId(),
                    d.getCustomerOrder().getId(),
                    d.getProduct().getId(),
                    d.getProduct().getName(),
                    d.getHowSold() == null ? null : d.getHowSold().getId(),
                    d.getHowSold() == null ? null : d.getHowSold().getName(),
                    d.getQuantity(),
                    d.getSalesPrice(),
                    d.getSalesPrice().multiply(BigDecimal.valueOf(d.getQuantity())),
                    d.getProduct().getBasePrice(),
                    base.baseUnit() == null ? null : base.baseUnit().getName(),
                    needed,
                    availableQuantity,
                    availableQuantity >= needed,
                    d.isActive(),
                    d.getCreatedAt(),
                    d.getUpdatedAt());
        }
    }
}
