package com.example.warehouse_proj.shipment;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends BaseRepository<Shipment> {

    @Override
    @EntityGraph(attributePaths = {"customerOrder", "truck", "employee"})
    List<Shipment> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = {"customerOrder", "truck", "employee"})
    Page<Shipment> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customerOrder", "truck", "employee"})
    Optional<Shipment> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"customerOrder", "truck", "employee"})
    List<Shipment> findAllByCustomerOrderIdAndActiveTrue(UUID orderId);

    @EntityGraph(attributePaths = {"customerOrder", "truck", "employee"})
    List<Shipment> findAllByTruckIdAndActiveTrue(UUID truckId);

    // Used by the parents' delete guards: you cannot delete something that still has shipments.
    boolean existsByCustomerOrderIdAndActiveTrue(UUID orderId);

    boolean existsByTruckIdAndActiveTrue(UUID truckId);

    boolean existsByEmployeeIdAndActiveTrue(UUID employeeId);
}
