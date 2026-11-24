package com.example.warehouse_proj.aisle;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.shelf.ShelfRepository;
import com.example.warehouse_proj.warehouse.Warehouse;
import com.example.warehouse_proj.warehouse.WarehouseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AisleServiceImpl implements AisleService {

    private static final String ENTITY = "Aisle";
    private static final Set<String> SORT_FIELDS = Set.of("name");

    private final AisleRepository aisles;
    private final WarehouseRepository warehouses;
    private final ShelfRepository shelves;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public AisleServiceImpl(
            AisleRepository aisles,
            WarehouseRepository warehouses,
            ShelfRepository shelves,
            AuditLogService audit,
            PaginationService pagination) {
        this.aisles = aisles;
        this.warehouses = warehouses;
        this.shelves = shelves;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AisleDtos.Response> list(UUID warehouseId) {
        List<Aisle> result = warehouseId == null
                ? aisles.findAllByActiveTrue()
                : aisles.findAllByWarehouseIdAndActiveTrue(warehouseId);
        return result.stream().map(AisleDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<AisleDtos.Response> page(PageQuery query) {
        return pagination.page(aisles, query, SORT_FIELDS, AisleDtos.Response::from);
    }

    @Transactional(readOnly = true)
    @Override
    public AisleDtos.Response get(UUID id) {
        return AisleDtos.Response.from(find(id));
    }

    @Transactional
    @Override
    public AisleDtos.Response create(AisleDtos.Request request) {
        Aisle aisle = new Aisle(findWarehouse(request.warehouseId()), request.name().trim());
        aisles.save(aisle);
        audit.record(AuditAction.CREATE, ENTITY, aisle.getId());
        return AisleDtos.Response.from(aisle);
    }

    @Transactional
    @Override
    public AisleDtos.Response update(UUID id, AisleDtos.Request request) {
        Aisle aisle = find(id);
        aisle.setWarehouse(findWarehouse(request.warehouseId()));
        aisle.setName(request.name().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        aisles.saveAndFlush(aisle);
        audit.record(AuditAction.UPDATE, ENTITY, aisle.getId());
        return AisleDtos.Response.from(aisle);
    }

    /** Soft delete; refused while the aisle still has active shelves. */
    @Transactional
    @Override
    public void delete(UUID id) {
        Aisle aisle = find(id);
        if (shelves.existsByAisleIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aisle still has shelves");
        }
        aisle.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private Aisle find(UUID id) {
        return aisles.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aisle not found"));
    }

    private Warehouse findWarehouse(UUID id) {
        return warehouses.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
    }
}
