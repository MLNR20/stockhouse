package com.example.warehouse_proj.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record Request(
            @NotBlank @Size(max = 200) String supplierName,
            @NotBlank @Size(max = 30) String phoneNumber,
            @NotBlank @Email @Size(max = 256) String email) {
    }

    public record Response(
            UUID id,
            String supplierName,
            String phoneNumber,
            String email,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            Set<UUID> productIds) {

        static Response from(Supplier s, Set<UUID> productIds) {
            return new Response(
                    s.getId(),
                    s.getSupplierName(),
                    s.getPhoneNumber(),
                    s.getEmail(),
                    s.isActive(),
                    s.getCreatedAt(),
                    s.getUpdatedAt(),
                    productIds);
        }
    }
}
