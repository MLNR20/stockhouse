package com.example.warehouse_proj.howsold;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.common.ActiveUniqueness;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class HowSoldService {

    private static final String ENTITY = "HowSold";
    private static final Set<String> SORT_FIELDS = Set.of("name");
    private static final List<String> SEARCH_FIELDS = List.of(
            "name");

    private final HowSoldRepository howSolds;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public HowSoldService(HowSoldRepository howSolds, AuditLogService audit, PaginationService pagination) {
        this.howSolds = howSolds;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<HowSoldDtos.Response> list() {
        return howSolds.findAllByActiveTrue().stream().map(HowSoldDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<HowSoldDtos.Response> page(PageQuery query) {
        return pagination.page(howSolds, query, SORT_FIELDS, SEARCH_FIELDS, HowSoldDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public HowSoldDtos.Response get(UUID id) {
        return HowSoldDtos.Response.from(find(id));
    }

    @Transactional
    public HowSoldDtos.Response create(HowSoldDtos.Request request) {
        String name = request.name().trim();
        requireNameFree(name, null);
        HowSold howSold = howSolds.save(new HowSold(name));
        audit.record(AuditAction.CREATE, ENTITY, howSold.getId());
        return HowSoldDtos.Response.from(howSold);
    }

    @Transactional
    public HowSoldDtos.Response update(UUID id, HowSoldDtos.Request request) {
        HowSold howSold = find(id);
        String name = request.name().trim();
        requireNameFree(name, id);
        howSold.setName(name);
        // Flush so @PreUpdate has set updatedAt before the response is built.
        howSolds.saveAndFlush(howSold);
        audit.record(AuditAction.UPDATE, ENTITY, howSold.getId());
        return HowSoldDtos.Response.from(howSold);
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID id) {
        find(id).setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    /** One active unit per name; a deleted unit's name can be used again. */
    private void requireNameFree(String name, UUID excludeId) {
        ActiveUniqueness.require(
                howSolds, "name", name, excludeId, "A unit named \"" + name + "\" already exists");
    }

    private HowSold find(UUID id) {
        return howSolds.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit of measure not found"));
    }
}
