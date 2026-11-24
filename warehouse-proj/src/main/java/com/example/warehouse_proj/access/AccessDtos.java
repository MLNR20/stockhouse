package com.example.warehouse_proj.access;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AccessDtos {

    private AccessDtos() {
    }

    public record PageInfo(String path, String label, String section) {
    }

    /** Everything that can be granted, for the admin screens. */
    public record CatalogResponse(List<String> resources, List<String> actions, List<PageInfo> pages) {
    }

    /** What one user may do: sent with /api/auth/me. */
    public record AccessSummary(String role, Set<String> permissions, Set<String> pages) {
    }

    public record RoleRequest(
            @NotBlank @Size(max = 50) String name,
            @Size(max = 255) String description,
            Set<String> permissions,
            Set<String> pages) {
    }

    public record RolePagesRequest(@NotNull Set<String> pages) {
    }

    public record RoleResponse(
            UUID id,
            String name,
            String description,
            boolean system,
            List<String> permissions,
            List<String> pages,
            long userCount,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record UserRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 256) String email,
            /** Required when adding a user; leave empty when editing to keep the current password. */
            @Size(min = 8, max = 72) String password,
            @NotNull UUID roleId) {
    }

    public record UserResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            UUID roleId,
            String roleName,
            Instant createdAt,
            Instant updatedAt) {
    }
}
