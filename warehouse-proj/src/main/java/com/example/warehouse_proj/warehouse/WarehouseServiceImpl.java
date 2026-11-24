package com.example.warehouse_proj.warehouse;

import com.example.warehouse_proj.aisle.AisleRepository;
import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.employee.WarehouseEmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private static final String ENTITY = "Warehouse";
    private static final Set<String> SORT_FIELDS = Set.of("name");

    private final WarehouseRepository warehouses;
    private final AisleRepository aisles;
    private final AuditLogService audit;
    private final PaginationService pagination;
    private final WarehouseEmployeeRepository employees;

    public WarehouseServiceImpl(
            WarehouseRepository warehouses,
            AisleRepository aisles,
            AuditLogService audit,
            PaginationService pagination,
            WarehouseEmployeeRepository employees) {
        this.warehouses = warehouses;
        this.aisles = aisles;
        this.audit = audit;
        this.pagination = pagination;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    @Override
    public List<WarehouseDtos.Response> list() {
        return warehouses.findAllByActiveTrue().stream().map(WarehouseDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<WarehouseDtos.Response> page(PageQuery query) {
        return pagination.page(warehouses, query, SORT_FIELDS, WarehouseDtos.Response::from);
    }

    @Transactional(readOnly = true)
    @Override
    public WarehouseDtos.Response get(UUID id) {
        return WarehouseDtos.Response.from(find(id));
    }

    @Transactional
    @Override
    public WarehouseDtos.Response create(WarehouseDtos.Request request) {
        Warehouse warehouse = new Warehouse(request.name().trim());
        warehouses.save(warehouse);
        audit.record(AuditAction.CREATE, ENTITY, warehouse.getId());
        return WarehouseDtos.Response.from(warehouse);
    }

    @Transactional
    @Override
    public WarehouseDtos.Response update(UUID id, WarehouseDtos.Request request) {
        Warehouse warehouse = find(id);
        warehouse.setName(request.name().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        warehouses.saveAndFlush(warehouse);
        audit.record(AuditAction.UPDATE, ENTITY, warehouse.getId());
        return WarehouseDtos.Response.from(warehouse);
    }

    /** Soft delete; refused while the warehouse still has active aisles. */
    @Transactional
    @Override
    public void delete(UUID id) {
        Warehouse warehouse = find(id);
        if (aisles.existsByWarehouseIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse still has aisles");
        }
        if (employees.existsByWarehouseIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse still has employees");
        }
        warehouse.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private Warehouse find(UUID id) {
        return warehouses.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
    }
}
