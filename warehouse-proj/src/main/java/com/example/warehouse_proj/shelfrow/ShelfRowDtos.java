package com.example.warehouse_proj.shelfrow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ShelfRowDtos {

    private ShelfRowDtos() {
    }

    public record Request(@NotNull UUID shelfId, @NotBlank @Size(max = 100) String name) {
    }

    public record Response(
            UUID id, UUID shelfId, String name, boolean active, Instant createdAt, Instant updatedAt) {

        static Response from(ShelfRow r) {
            return new Response(
                    r.getId(), r.getShelf().getId(), r.getName(), r.isActive(),
                    r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
