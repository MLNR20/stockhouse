package com.example.warehouse_proj.howsold;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class HowSoldDtos {

    private HowSoldDtos() {
    }

    public record Request(@NotBlank @Size(max = 100) String name) {
    }

    public record Response(
            UUID id,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(HowSold h) {
            return new Response(h.getId(), h.getName(), h.isActive(), h.getCreatedAt(), h.getUpdatedAt());
        }
    }
}
