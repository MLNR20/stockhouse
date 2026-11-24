package com.example.warehouse_proj.aisle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AisleDtos {

    private AisleDtos() {
    }

    public record Request(@NotNull UUID warehouseId, @NotBlank @Size(max = 100) String name) {
    }

    public record Response(
            UUID id, UUID warehouseId, String name, boolean active, Instant createdAt, Instant updatedAt) {

        static Response from(Aisle a) {
            return new Response(
                    a.getId(), a.getWarehouse().getId(), a.getName(), a.isActive(),
                    a.getCreatedAt(), a.getUpdatedAt());
        }
    }
}
