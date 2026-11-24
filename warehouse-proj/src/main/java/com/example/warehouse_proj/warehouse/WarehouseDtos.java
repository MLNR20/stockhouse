package com.example.warehouse_proj.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class WarehouseDtos {

    private WarehouseDtos() {
    }

    public record Request(@NotBlank @Size(max = 200) String name) {
    }

    public record Response(UUID id, String name, boolean active, Instant createdAt, Instant updatedAt) {

        static Response from(Warehouse w) {
            return new Response(w.getId(), w.getName(), w.isActive(), w.getCreatedAt(), w.getUpdatedAt());
        }
    }
}
