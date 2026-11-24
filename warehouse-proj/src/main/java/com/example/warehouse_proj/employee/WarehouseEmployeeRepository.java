package com.example.warehouse_proj.employee;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseEmployeeRepository extends BaseRepository<WarehouseEmployee> {

    @Override
    @EntityGraph(attributePaths = "warehouse")
    List<WarehouseEmployee> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "warehouse")
    Page<WarehouseEmployee> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "warehouse")
    Optional<WarehouseEmployee> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = "warehouse")
    List<WarehouseEmployee> findAllByWarehouseIdAndActiveTrue(UUID warehouseId);

    boolean existsByWarehouseIdAndActiveTrue(UUID warehouseId);
}
