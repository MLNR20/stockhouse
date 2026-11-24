package com.example.warehouse_proj.employee;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.shipment.ShipmentRepository;
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
public class WarehouseEmployeeService {

    private static final String ENTITY = "WarehouseEmployee";
    private static final Set<String> SORT_FIELDS = Set.of("firstName", "lastName", "email", "phoneNumber");

    private final WarehouseEmployeeRepository employees;
    private final WarehouseRepository warehouses;
    private final ShipmentRepository shipments;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public WarehouseEmployeeService(
            WarehouseEmployeeRepository employees,
            WarehouseRepository warehouses,
            ShipmentRepository shipments,
            AuditLogService audit,
            PaginationService pagination) {
        this.employees = employees;
        this.warehouses = warehouses;
        this.shipments = shipments;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<WarehouseEmployeeDtos.Response> list(UUID warehouseId) {
        List<WarehouseEmployee> result = warehouseId == null
                ? employees.findAllByActiveTrue()
                : employees.findAllByWarehouseIdAndActiveTrue(warehouseId);
        return result.stream().map(WarehouseEmployeeDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseEmployeeDtos.Response> page(PageQuery query) {
        return pagination.page(employees, query, SORT_FIELDS, WarehouseEmployeeDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public WarehouseEmployeeDtos.Response get(UUID id) {
        return WarehouseEmployeeDtos.Response.from(find(id));
    }

    @Transactional
    public WarehouseEmployeeDtos.Response create(WarehouseEmployeeDtos.Request request) {
        WarehouseEmployee employee = new WarehouseEmployee(
                findWarehouse(request.warehouseId()),
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim(),
                request.phoneNumber().trim());
        employees.save(employee);
        audit.record(AuditAction.CREATE, ENTITY, employee.getId());
        return WarehouseEmployeeDtos.Response.from(employee);
    }

    @Transactional
    public WarehouseEmployeeDtos.Response update(UUID id, WarehouseEmployeeDtos.Request request) {
        WarehouseEmployee employee = find(id);
        employee.setWarehouse(findWarehouse(request.warehouseId()));
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(request.email().trim());
        employee.setPhoneNumber(request.phoneNumber().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        employees.saveAndFlush(employee);
        audit.record(AuditAction.UPDATE, ENTITY, employee.getId());
        return WarehouseEmployeeDtos.Response.from(employee);
    }

    /** Soft delete; refused while the employee is still assigned to active shipments. */
    @Transactional
    public void delete(UUID id) {
        WarehouseEmployee employee = find(id);
        if (shipments.existsByEmployeeIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is still assigned to shipments");
        }
        employee.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private WarehouseEmployee find(UUID id) {
        return employees.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private Warehouse findWarehouse(UUID id) {
        return warehouses.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
    }
}
