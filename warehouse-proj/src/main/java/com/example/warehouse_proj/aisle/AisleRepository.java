package com.example.warehouse_proj.aisle;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AisleRepository extends BaseRepository<Aisle> {

    @Override
    @EntityGraph(attributePaths = "warehouse")
    List<Aisle> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "warehouse")
    Page<Aisle> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "warehouse")
    Optional<Aisle> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = "warehouse")
    List<Aisle> findAllByWarehouseIdAndActiveTrue(UUID warehouseId);

    boolean existsByWarehouseIdAndActiveTrue(UUID warehouseId);
}
