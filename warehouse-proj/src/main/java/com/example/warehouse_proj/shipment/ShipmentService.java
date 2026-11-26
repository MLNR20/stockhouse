package com.example.warehouse_proj.shipment;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.employee.WarehouseEmployee;
import com.example.warehouse_proj.employee.WarehouseEmployeeRepository;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.order.OrderRepository;
import com.example.warehouse_proj.order.OrderStatus;
import com.example.warehouse_proj.truck.Truck;
import com.example.warehouse_proj.truck.TruckRepository;
import com.example.warehouse_proj.truck.TruckStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ShipmentService {

    private static final String ENTITY = "Shipment";
    private static final Set<String> SORT_FIELDS = Set.of("scheduledDate", "status");
    private static final List<String> SEARCH_FIELDS = List.of(
            "status", "scheduledDate", "truck.plateNumber", "employee.firstName", "employee.lastName", "customerOrder.customer.firstName", "customerOrder.customer.lastName");

    private final ShipmentRepository shipments;
    private final OrderRepository orders;
    private final TruckRepository trucks;
    private final WarehouseEmployeeRepository employees;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public ShipmentService(
            ShipmentRepository shipments,
            OrderRepository orders,
            TruckRepository trucks,
            WarehouseEmployeeRepository employees,
            AuditLogService audit,
            PaginationService pagination) {
        this.shipments = shipments;
        this.orders = orders;
        this.trucks = trucks;
        this.employees = employees;
        this.audit = audit;
        this.pagination = pagination;
    }

    /** All active shipments, optionally only those of one order and/or one truck. */
    @Transactional(readOnly = true)
    public List<ShipmentDtos.Response> list(UUID orderId, UUID truckId) {
        List<Shipment> result;
        if (orderId != null) {
            result = shipments.findAllByCustomerOrderIdAndActiveTrue(orderId);
        } else if (truckId != null) {
            result = shipments.findAllByTruckIdAndActiveTrue(truckId);
        } else {
            result = shipments.findAllByActiveTrue();
        }
        return result.stream()
                .filter(s -> truckId == null || s.getTruck().getId().equals(truckId))
                .map(ShipmentDtos.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentDtos.Response> page(PageQuery query) {
        return pagination.page(shipments, query, SORT_FIELDS, SEARCH_FIELDS, ShipmentDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public ShipmentDtos.Response get(UUID id) {
        return ShipmentDtos.Response.from(find(id));
    }

    @Transactional
    public ShipmentDtos.Response create(ShipmentDtos.Request request) {
        CustomerOrder order = findOrder(request.orderId());
        requireShippable(order);
        Truck truck = findTruck(request.truckId());
        requireUsable(truck);

        Shipment shipment = new Shipment(
                order,
                truck,
                findEmployee(request.employeeId()),
                request.scheduledDate(),
                request.status() != null ? request.status() : ShipmentStatus.SCHEDULED);
        shipments.save(shipment);
        audit.record(AuditAction.CREATE, ENTITY, shipment.getId());
        return ShipmentDtos.Response.from(shipment);
    }

    @Transactional
    public ShipmentDtos.Response update(UUID id, ShipmentDtos.Request request) {
        Shipment shipment = find(id);

        // Only re-check an order or truck that is being changed: an existing shipment must stay editable
        // even if its truck later went into maintenance or its order was cancelled.
        if (!shipment.getCustomerOrder().getId().equals(request.orderId())) {
            CustomerOrder order = findOrder(request.orderId());
            requireShippable(order);
            shipment.setCustomerOrder(order);
        }
        if (!shipment.getTruck().getId().equals(request.truckId())) {
            Truck truck = findTruck(request.truckId());
            requireUsable(truck);
            shipment.setTruck(truck);
        }
        shipment.setEmployee(findEmployee(request.employeeId()));
        shipment.setScheduledDate(request.scheduledDate());
        if (request.status() != null) {
            shipment.setStatus(request.status());
        }
        // Flush so @PreUpdate has set updatedAt before the response is built.
        shipments.saveAndFlush(shipment);
        audit.record(AuditAction.UPDATE, ENTITY, shipment.getId());
        return ShipmentDtos.Response.from(shipment);
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID id) {
        find(id).setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private static void requireShippable(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cancelled order cannot be shipped");
        }
    }

    private static void requireUsable(Truck truck) {
        if (truck.getStatus() == TruckStatus.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That truck is under maintenance");
        }
    }

    private Shipment find(UUID id) {
        return shipments.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
    }

    private CustomerOrder findOrder(UUID id) {
        return orders.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private Truck findTruck(UUID id) {
        return trucks.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Truck not found"));
    }

    /** Null means "not assigned yet". */
    private WarehouseEmployee findEmployee(UUID id) {
        if (id == null) {
            return null;
        }
        return employees.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }
}
