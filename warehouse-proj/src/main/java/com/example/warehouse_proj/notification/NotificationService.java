package com.example.warehouse_proj.notification;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** Clients subscribe to /user/queue/notifications; Spring routes it to that user's sessions only. */
    static final String DESTINATION = "/queue/notifications";

    private static final Set<String> SORT_FIELDS = Set.of("type", "title", "read");

    private final NotificationRepository notifications;
    private final AppUserRepository users;
    private final SimpMessagingTemplate messaging;
    private final PaginationService pagination;

    public NotificationService(
            NotificationRepository notifications,
            AppUserRepository users,
            SimpMessagingTemplate messaging,
            PaginationService pagination) {
        this.notifications = notifications;
        this.users = users;
        this.messaging = messaging;
        this.pagination = pagination;
    }

    // ---- producing notifications (called by other services) ------------------------------------

    /**
     * Creates one notification per active user and pushes each over WebSocket once the surrounding
     * transaction commits (so clients never get a push for something that was rolled back).
     * Call it from inside a @Transactional service method.
     */
    @Transactional
    public void notifyAllUsers(
            NotificationType type, String title, String message, String entityName, UUID entityId) {
        List<Notification> created = users.findAllByActiveTrue().stream()
                .map(u -> new Notification(u.getId(), type, title, message, entityName, entityId))
                .toList();
        if (created.isEmpty()) {
            return;
        }
        notifications.saveAll(created);
        pushAfterCommit(created);
    }

    private void pushAfterCommit(List<Notification> created) {
        Runnable push = () -> {
            for (Notification n : created) {
                try {
                    messaging.convertAndSendToUser(
                            n.getRecipientId().toString(), DESTINATION, NotificationDtos.Response.from(n));
                } catch (RuntimeException e) {
                    // The notification is saved; a failed live push must not fail the request.
                    log.warn("Could not push notification {} to user {}", n.getId(), n.getRecipientId(), e);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }

    // ---- reading and managing one user's notifications -----------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationDtos.Response> list(UUID userId, boolean unreadOnly) {
        List<Notification> result = unreadOnly
                ? notifications.findAllByRecipientIdAndReadFalseAndActiveTrueOrderByCreatedAtDesc(userId)
                : notifications.findAllByRecipientIdAndActiveTrueOrderByCreatedAtDesc(userId);
        return result.stream().map(NotificationDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDtos.Response> page(UUID userId, PageQuery query) {
        return PageResponse.of(
                notifications.findAllByRecipientIdAndActiveTrue(userId, pagination.toPageable(query, SORT_FIELDS)),
                NotificationDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCount unreadCount(UUID userId) {
        return new NotificationDtos.UnreadCount(notifications.countByRecipientIdAndReadFalseAndActiveTrue(userId));
    }

    @Transactional(readOnly = true)
    public NotificationDtos.Response get(UUID userId, UUID id) {
        return NotificationDtos.Response.from(find(userId, id));
    }

    @Transactional
    public NotificationDtos.Response markRead(UUID userId, UUID id) {
        Notification notification = find(userId, id);
        notification.markRead();
        // Flush so @PreUpdate has set updatedAt before the response is built.
        notifications.saveAndFlush(notification);
        return NotificationDtos.Response.from(notification);
    }

    /** @return how many notifications were newly marked as read */
    @Transactional
    public int markAllRead(UUID userId) {
        List<Notification> unread =
                notifications.findAllByRecipientIdAndReadFalseAndActiveTrueOrderByCreatedAtDesc(userId);
        unread.forEach(Notification::markRead);
        return unread.size();
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID userId, UUID id) {
        find(userId, id).setActive(false);
    }

    private Notification find(UUID userId, UUID id) {
        return notifications.findByIdAndRecipientIdAndActiveTrue(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }
}
