package com.example.warehouse_proj.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record Request(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description) {
    }

    public record Response(
            UUID id,
            String name,
            String description,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(Category c) {
            return new Response(
                    c.getId(), c.getName(), c.getDescription(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
