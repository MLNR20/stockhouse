package com.example.warehouse_proj.notification;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends BaseRepository<Notification> {

    List<Notification> findAllByRecipientIdAndActiveTrueOrderByCreatedAtDesc(UUID recipientId);

    List<Notification> findAllByRecipientIdAndReadFalseAndActiveTrueOrderByCreatedAtDesc(UUID recipientId);

    Page<Notification> findAllByRecipientIdAndActiveTrue(UUID recipientId, Pageable pageable);

    /** Scoped to the recipient, so a user can never touch someone else's notification by id. */
    Optional<Notification> findByIdAndRecipientIdAndActiveTrue(UUID id, UUID recipientId);

    long countByRecipientIdAndReadFalseAndActiveTrue(UUID recipientId);
}
