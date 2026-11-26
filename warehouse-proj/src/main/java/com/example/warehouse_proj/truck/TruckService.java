package com.example.warehouse_proj.truck;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.shipment.ShipmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TruckService {

    private static final String ENTITY = "Truck";
    private static final Set<String> SORT_FIELDS = Set.of("plateNumber", "model", "capacityKg", "status");
    private static final List<String> SEARCH_FIELDS = List.of(
            "plateNumber", "model", "status");

    private final TruckRepository trucks;
    private final ShipmentRepository shipments;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public TruckService(
            TruckRepository trucks,
            ShipmentRepository shipments,
            AuditLogService audit,
            PaginationService pagination) {
        this.trucks = trucks;
        this.shipments = shipments;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<TruckDtos.Response> list() {
        return trucks.findAllByActiveTrue().stream().map(TruckDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TruckDtos.Response> page(PageQuery query) {
        return pagination.page(trucks, query, SORT_FIELDS, SEARCH_FIELDS, TruckDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public TruckDtos.Response get(UUID id) {
        return TruckDtos.Response.from(find(id));
    }

    @Transactional
    public TruckDtos.Response create(TruckDtos.Request request) {
        String plate = request.plateNumber().trim();
        if (trucks.existsByPlateNumberIgnoreCaseAndActiveTrue(plate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A truck with this plate number already exists");
        }
        Truck truck = new Truck(
                plate,
                request.model().trim(),
                request.capacityKg(),
                request.status() != null ? request.status() : TruckStatus.AVAILABLE);
        trucks.save(truck);
        audit.record(AuditAction.CREATE, ENTITY, truck.getId());
        return TruckDtos.Response.from(truck);
    }

    @Transactional
    public TruckDtos.Response update(UUID id, TruckDtos.Request request) {
        Truck truck = find(id);
        String plate = request.plateNumber().trim();
        if (trucks.existsByPlateNumberIgnoreCaseAndActiveTrueAndIdNot(plate, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A truck with this plate number already exists");
        }
        truck.setPlateNumber(plate);
        truck.setModel(request.model().trim());
        truck.setCapacityKg(request.capacityKg());
        if (request.status() != null) {
            truck.setStatus(request.status());
        }
        // Flush so @PreUpdate has set updatedAt before the response is built.
        trucks.saveAndFlush(truck);
        audit.record(AuditAction.UPDATE, ENTITY, truck.getId());
        return TruckDtos.Response.from(truck);
    }

    /** Soft delete; refused while the truck still has active shipments. */
    @Transactional
    public void delete(UUID id) {
        Truck truck = find(id);
        if (shipments.existsByTruckIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Truck still has shipments");
        }
        truck.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private Truck find(UUID id) {
        return trucks.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Truck not found"));
    }
}
