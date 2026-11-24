package com.example.warehouse_proj.audit;

import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final String SYSTEM = "system";
    private static final String SYSTEM_NAME = "System";

    public static final int DEFAULT_LIMIT = 500;
    public static final int MAX_LIMIT = 2000;

    private final AuditLogRepository auditLogs;
    private final AppUserRepository users;

    public AuditLogService(AuditLogRepository auditLogs, AppUserRepository users) {
        this.auditLogs = auditLogs;
        this.users = users;
    }

    /**
     * Records an action on the current transaction, so the log row commits or rolls back
     * together with the change it describes. Call it from inside a @Transactional service method.
     */
    @Transactional
    public void record(AuditAction action, String entityName, UUID entityId) {
        auditLogs.save(new AuditLog(entityName, entityId, action, currentUser()));
    }

    /** The most recent {@code limit} entries (newest first), optionally for one kind or one record. */
    @Transactional(readOnly = true)
    public List<AuditLogDtos.Response> list(String entityName, UUID entityId, int limit) {
        Pageable newestFirst = PageRequest.of(
                0, Math.min(Math.max(limit, 1), MAX_LIMIT),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")));

        Page<AuditLog> page;
        if (entityName != null && entityId != null) {
            page = auditLogs.findAllByEntityNameAndEntityId(entityName, entityId, newestFirst);
        } else if (entityName != null) {
            page = auditLogs.findAllByEntityName(entityName, newestFirst);
        } else if (entityId != null) {
            page = auditLogs.findAllByEntityId(entityId, newestFirst);
        } else {
            page = auditLogs.findAll(newestFirst);
        }
        return withUserNames(page.getContent());
    }

    /** Turns the stored user ids into names with one lookup for all of them. */
    private List<AuditLogDtos.Response> withUserNames(List<AuditLog> entries) {
        Set<UUID> userIds = new HashSet<>();
        for (AuditLog entry : entries) {
            parseUuid(entry.getPerformedBy()).ifPresent(userIds::add);
        }
        Map<UUID, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (AppUser user : users.findAllById(userIds)) {
                String full = (user.getFirstName() + " " + user.getLastName()).trim();
                names.put(user.getId(), full.isEmpty() ? user.getEmail() : full);
            }
        }
        return entries.stream()
                .map(entry -> AuditLogDtos.Response.from(entry, displayName(entry.getPerformedBy(), names)))
                .toList();
    }

    private static String displayName(String performedBy, Map<UUID, String> names) {
        if (SYSTEM.equals(performedBy)) {
            return SYSTEM_NAME;
        }
        return parseUuid(performedBy).map(id -> names.getOrDefault(id, performedBy)).orElse(performedBy);
    }

    private static java.util.Optional<UUID> parseUuid(String value) {
        try {
            return java.util.Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && authentication.getName() != null
                ? authentication.getName()
                : SYSTEM;
    }
}
