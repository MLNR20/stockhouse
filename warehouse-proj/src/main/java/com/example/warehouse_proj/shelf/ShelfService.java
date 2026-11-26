package com.example.warehouse_proj.shelf;

import com.example.warehouse_proj.aisle.Aisle;
import com.example.warehouse_proj.aisle.AisleRepository;
import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.shelfrow.ShelfRowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ShelfService {

    private static final String ENTITY = "Shelf";
    private static final Set<String> SORT_FIELDS = Set.of("name");
    private static final List<String> SEARCH_FIELDS = List.of(
            "name", "aisle.name", "aisle.warehouse.name");

    private final ShelfRepository shelves;
    private final AisleRepository aisles;
    private final ShelfRowRepository rows;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public ShelfService(
            ShelfRepository shelves,
            AisleRepository aisles,
            ShelfRowRepository rows,
            AuditLogService audit,
            PaginationService pagination) {
        this.shelves = shelves;
        this.aisles = aisles;
        this.rows = rows;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<ShelfDtos.Response> list(UUID aisleId) {
        List<Shelf> result = aisleId == null
                ? shelves.findAllByActiveTrue()
                : shelves.findAllByAisleIdAndActiveTrue(aisleId);
        return result.stream().map(ShelfDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ShelfDtos.Response> page(PageQuery query) {
        return pagination.page(shelves, query, SORT_FIELDS, SEARCH_FIELDS, ShelfDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public ShelfDtos.Response get(UUID id) {
        return ShelfDtos.Response.from(find(id));
    }

    @Transactional
    public ShelfDtos.Response create(ShelfDtos.Request request) {
        Shelf shelf = new Shelf(findAisle(request.aisleId()), request.name().trim());
        shelves.save(shelf);
        audit.record(AuditAction.CREATE, ENTITY, shelf.getId());
        return ShelfDtos.Response.from(shelf);
    }

    @Transactional
    public ShelfDtos.Response update(UUID id, ShelfDtos.Request request) {
        Shelf shelf = find(id);
        shelf.setAisle(findAisle(request.aisleId()));
        shelf.setName(request.name().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        shelves.saveAndFlush(shelf);
        audit.record(AuditAction.UPDATE, ENTITY, shelf.getId());
        return ShelfDtos.Response.from(shelf);
    }

    /** Soft delete; refused while the shelf still has active rows. */
    @Transactional
    public void delete(UUID id) {
        Shelf shelf = find(id);
        if (rows.existsByShelfIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shelf still has rows");
        }
        shelf.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private Shelf find(UUID id) {
        return shelves.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shelf not found"));
    }

    private Aisle findAisle(UUID id) {
        return aisles.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aisle not found"));
    }
}
