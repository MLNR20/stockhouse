package com.example.warehouse_proj.employee;

import com.example.warehouse_proj.common.NameRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class WarehouseEmployeeDtos {

    private WarehouseEmployeeDtos() {
    }

    public record Request(
            @NotNull UUID warehouseId,
            @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = NameRules.LETTERS_ONLY, message = NameRules.LETTERS_ONLY_MESSAGE)
            String firstName,
            @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = NameRules.LETTERS_ONLY, message = NameRules.LETTERS_ONLY_MESSAGE)
            String lastName,
            @NotBlank @Email @Size(max = 256) String email,
            @NotBlank @Size(max = 30) String phoneNumber) {
    }

    public record Response(
            UUID id,
            UUID warehouseId,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(WarehouseEmployee e) {
            return new Response(
                    e.getId(),
                    e.getWarehouse().getId(),
                    e.getFirstName(),
                    e.getLastName(),
                    e.getEmail(),
                    e.getPhoneNumber(),
                    e.isActive(),
                    e.getCreatedAt(),
                    e.getUpdatedAt());
        }
    }
}
