package com.example.warehouse_proj.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Read-only: audit rows are written by the services, never through the API. This is the user history. */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    /**
     * Newest first, at most {@code limit} entries (default 500, max 2000).
     * Optional filters: ?entityName=Customer&entityId=...
     */
    @GetMapping
    public List<AuditLogDtos.Response> list(
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "" + AuditLogService.DEFAULT_LIMIT) int limit) {
        return service.list(entityName, entityId, limit);
    }
}
