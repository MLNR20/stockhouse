package com.example.warehouse_proj.shelfrow;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.container.ContainerRepository;
import com.example.warehouse_proj.shelf.Shelf;
import com.example.warehouse_proj.shelf.ShelfRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ShelfRowService {

    private static final String ENTITY = "ShelfRow";
    private static final Set<String> SORT_FIELDS = Set.of("name");

    private final ShelfRowRepository rows;
    private final ShelfRepository shelves;
    private final ContainerRepository containers;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public ShelfRowService(
            ShelfRowRepository rows,
            ShelfRepository shelves,
            ContainerRepository containers,
            AuditLogService audit,
            PaginationService pagination) {
        this.rows = rows;
        this.shelves = shelves;
        this.containers = containers;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<ShelfRowDtos.Response> list(UUID shelfId) {
        List<ShelfRow> result = shelfId == null
                ? rows.findAllByActiveTrue()
                : rows.findAllByShelfIdAndActiveTrue(shelfId);
        return result.stream().map(ShelfRowDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ShelfRowDtos.Response> page(PageQuery query) {
        return pagination.page(rows, query, SORT_FIELDS, ShelfRowDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public ShelfRowDtos.Response get(UUID id) {
        return ShelfRowDtos.Response.from(find(id));
    }

    @Transactional
    public ShelfRowDtos.Response create(ShelfRowDtos.Request request) {
        ShelfRow row = new ShelfRow(findShelf(request.shelfId()), request.name().trim());
        rows.save(row);
        audit.record(AuditAction.CREATE, ENTITY, row.getId());
        return ShelfRowDtos.Response.from(row);
    }

    @Transactional
    public ShelfRowDtos.Response update(UUID id, ShelfRowDtos.Request request) {
        ShelfRow row = find(id);
        row.setShelf(findShelf(request.shelfId()));
        row.setName(request.name().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        rows.saveAndFlush(row);
        audit.record(AuditAction.UPDATE, ENTITY, row.getId());
        return ShelfRowDtos.Response.from(row);
    }

    /** Soft delete; refused while the row still has active containers. */
    @Transactional
    public void delete(UUID id) {
        ShelfRow row = find(id);
        if (containers.existsByShelfRowIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Row still has containers");
        }
        row.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private ShelfRow find(UUID id) {
        return rows.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Row not found"));
    }

    private Shelf findShelf(UUID id) {
        return shelves.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shelf not found"));
    }
}
