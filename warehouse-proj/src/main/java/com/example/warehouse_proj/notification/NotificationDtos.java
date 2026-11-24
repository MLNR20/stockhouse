package com.example.warehouse_proj.notification;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    /** Same shape for REST responses and for the STOMP push. */
    public record Response(
            UUID id,
            NotificationType type,
            String title,
            String message,
            String entityName,
            UUID entityId,
            boolean read,
            Instant readAt,
            Instant createdAt) {

        static Response from(Notification n) {
            return new Response(
                    n.getId(),
                    n.getType(),
                    n.getTitle(),
                    n.getMessage(),
                    n.getEntityName(),
                    n.getEntityId(),
                    n.isRead(),
                    n.getReadAt(),
                    n.getCreatedAt());
        }
    }

    public record UnreadCount(long count) {
    }
}
