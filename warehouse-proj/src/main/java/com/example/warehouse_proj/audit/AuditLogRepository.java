package com.example.warehouse_proj.audit;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** The Pageable carries both the sort (newest first) and the row limit, so nothing loads the whole table. */
public interface AuditLogRepository extends BaseRepository<AuditLog> {

    Page<AuditLog> findAllByEntityName(String entityName, Pageable pageable);

    Page<AuditLog> findAllByEntityId(UUID entityId, Pageable pageable);

    Page<AuditLog> findAllByEntityNameAndEntityId(String entityName, UUID entityId, Pageable pageable);
}
