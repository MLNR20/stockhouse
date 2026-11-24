package com.example.warehouse_proj.audit;

import java.time.Instant;
import java.util.UUID;

public final class AuditLogDtos {

    private AuditLogDtos() {
    }

    /**
     * @param performedBy     the user id (or "system")
     * @param performedByName the user's name for display, "System" for system actions, or the raw
     *                        value if that user no longer exists
     */
    public record Response(
            UUID id,
            String entityName,
            UUID entityId,
            AuditAction action,
            String performedBy,
            String performedByName,
            Instant createdAt) {

        static Response from(AuditLog a, String performedByName) {
            return new Response(
                    a.getId(),
                    a.getEntityName(),
                    a.getEntityId(),
                    a.getAction(),
                    a.getPerformedBy(),
                    performedByName,
                    a.getCreatedAt());
        }
    }
}
