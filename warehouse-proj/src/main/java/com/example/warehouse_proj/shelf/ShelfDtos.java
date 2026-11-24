package com.example.warehouse_proj.shelf;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ShelfDtos {

    private ShelfDtos() {
    }

    public record Request(@NotNull UUID aisleId, @NotBlank @Size(max = 100) String name) {
    }

    public record Response(
            UUID id, UUID aisleId, String name, boolean active, Instant createdAt, Instant updatedAt) {

        static Response from(Shelf s) {
            return new Response(
                    s.getId(), s.getAisle().getId(), s.getName(), s.isActive(),
                    s.getCreatedAt(), s.getUpdatedAt());
        }
    }
}
