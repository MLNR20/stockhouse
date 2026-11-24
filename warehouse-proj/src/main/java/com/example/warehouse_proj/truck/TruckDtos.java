package com.example.warehouse_proj.truck;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TruckDtos {

    private TruckDtos() {
    }

    /** status is optional: defaults to AVAILABLE on create, unchanged on update. */
    public record Request(
            @NotBlank @Size(max = 20) String plateNumber,
            @NotBlank @Size(max = 100) String model,
            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal capacityKg,
            TruckStatus status) {
    }

    public record Response(
            UUID id,
            String plateNumber,
            String model,
            BigDecimal capacityKg,
            TruckStatus status,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(Truck t) {
            return new Response(
                    t.getId(),
                    t.getPlateNumber(),
                    t.getModel(),
                    t.getCapacityKg(),
                    t.getStatus(),
                    t.isActive(),
                    t.getCreatedAt(),
                    t.getUpdatedAt());
        }
    }
}
