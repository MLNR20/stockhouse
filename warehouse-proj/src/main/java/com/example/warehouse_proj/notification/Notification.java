package com.example.warehouse_proj.notification;

import com.example.warehouse_proj.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An in-app notification for one user (recipientId is the AppUser id, i.e. the JWT subject).
 * entityName/entityId point at the record it is about, so a client can link to it.
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public Notification(
            UUID recipientId,
            NotificationType type,
            String title,
            String message,
            String entityName,
            UUID entityId) {
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.entityName = entityName;
        this.entityId = entityId;
    }

    /** Idempotent: keeps the first read time. */
    public void markRead() {
        if (!read) {
            read = true;
            readAt = Instant.now();
        }
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getEntityName() {
        return entityName;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
