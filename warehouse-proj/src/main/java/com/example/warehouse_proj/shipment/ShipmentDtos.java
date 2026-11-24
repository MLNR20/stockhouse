package com.example.warehouse_proj.shipment;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ShipmentDtos {

    private ShipmentDtos() {
    }

    /** employeeId is optional (not yet assigned); status defaults to SCHEDULED on create, unchanged on update. */
    public record Request(
            @NotNull UUID orderId,
            @NotNull UUID truckId,
            UUID employeeId,
            @NotNull LocalDate scheduledDate,
            ShipmentStatus status) {
    }

    public record Response(
            UUID id,
            UUID orderId,
            UUID truckId,
            UUID employeeId,
            LocalDate scheduledDate,
            ShipmentStatus status,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(Shipment s) {
            return new Response(
                    s.getId(),
                    s.getCustomerOrder().getId(),
                    s.getTruck().getId(),
                    s.getEmployee() == null ? null : s.getEmployee().getId(),
                    s.getScheduledDate(),
                    s.getStatus(),
                    s.isActive(),
                    s.getCreatedAt(),
                    s.getUpdatedAt());
        }
    }
}
