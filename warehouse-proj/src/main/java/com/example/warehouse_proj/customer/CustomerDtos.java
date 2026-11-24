package com.example.warehouse_proj.customer;

import com.example.warehouse_proj.common.NameRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record Request(
            @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = NameRules.LETTERS_ONLY, message = NameRules.LETTERS_ONLY_MESSAGE)
            String firstName,
            @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = NameRules.LETTERS_ONLY, message = NameRules.LETTERS_ONLY_MESSAGE)
            String lastName,
            @NotBlank @Email @Size(max = 256) String email,
            @NotBlank @Size(max = 30) String phoneNumber,
            @NotBlank @Size(max = 500) String address) {
    }

    public record Response(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String address,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        static Response from(Customer c) {
            return new Response(
                    c.getId(),
                    c.getFirstName(),
                    c.getLastName(),
                    c.getEmail(),
                    c.getPhoneNumber(),
                    c.getAddress(),
                    c.isActive(),
                    c.getCreatedAt(),
                    c.getUpdatedAt());
        }
    }
}
