package com.example.warehouse_proj.notification;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The signed-in user's own notifications. They are created by the system (see NotificationService),
 * so there is no POST. Live delivery is over WebSocket: connect to /ws, subscribe to /user/queue/notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /** Newest first. GET /api/notifications?unreadOnly=true */
    @GetMapping
    public List<NotificationDtos.Response> list(
            Authentication authentication, @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.list(userId(authentication), unreadOnly);
    }

    /** Paged: GET /api/notifications/page?page=0&size=20&sortBy=createdAt&direction=desc */
    @GetMapping("/page")
    public PageResponse<NotificationDtos.Response> page(
            Authentication authentication, @ModelAttribute PageQuery query) {
        return service.page(userId(authentication), query);
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCount unreadCount(Authentication authentication) {
        return service.unreadCount(userId(authentication));
    }

    @GetMapping("/{id}")
    public NotificationDtos.Response get(Authentication authentication, @PathVariable UUID id) {
        return service.get(userId(authentication), id);
    }

    @PutMapping("/{id}/read")
    public NotificationDtos.Response markRead(Authentication authentication, @PathVariable UUID id) {
        return service.markRead(userId(authentication), id);
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead(Authentication authentication) {
        return Map.of("updated", service.markAllRead(userId(authentication)));
    }

    /** Soft delete (sets isActive = false). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(userId(authentication), id);
    }

    /** The JWT subject is the user's id (see JwtService). */
    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
