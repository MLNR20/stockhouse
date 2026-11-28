package com.example.warehouse_proj.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    /** status is optional: defaults to PENDING on create, unchanged on update. */
    public record Request(
            @NotNull UUID customerId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal totalPrice,
            @NotNull LocalDate targetDeliveryDate,
            OrderStatus status) {
    }

    /** A deleted customer is still named on their orders, with customerActive = false. */
    public record Response(
            UUID id,
            UUID customerId,
            String customerName,
            boolean customerActive,
            BigDecimal totalPrice,
            LocalDate targetDeliveryDate,
            OrderStatus status,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(CustomerOrder o) {
            return new Response(
                    o.getId(),
                    o.getCustomer().getId(),
                    o.getCustomer().getFirstName() + " " + o.getCustomer().getLastName(),
                    o.getCustomer().isActive(),
                    o.getTotalPrice(),
                    o.getTargetDeliveryDate(),
                    o.getStatus(),
                    o.isActive(),
                    o.getCreatedAt(),
                    o.getUpdatedAt());
        }
    }
}
