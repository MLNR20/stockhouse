package com.example.warehouse_proj.container;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ContainerDtos {

    private ContainerDtos() {
    }

    public record Request(@NotNull UUID rowId, @NotBlank @Size(max = 100) String name) {
    }

    public record Response(
            UUID id, UUID rowId, String name, boolean active, Instant createdAt, Instant updatedAt) {

        static Response from(Container c) {
            return new Response(
                    c.getId(), c.getShelfRow().getId(), c.getName(), c.isActive(),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    public record ContentRequest(@NotNull @Positive Integer quantity) {
    }

    public record ContentResponse(
            UUID id,
            UUID containerId,
            UUID productId,
            int quantity,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static ContentResponse from(ContainerProduct cp) {
            return new ContentResponse(
                    cp.getId(),
                    cp.getContainer().getId(),
                    cp.getProduct().getId(),
                    cp.getQuantity(),
                    cp.isActive(),
                    cp.getCreatedAt(),
                    cp.getUpdatedAt());
        }
    }
}
