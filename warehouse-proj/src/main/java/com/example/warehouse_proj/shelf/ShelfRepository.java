package com.example.warehouse_proj.shelf;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShelfRepository extends BaseRepository<Shelf> {

    @Override
    @EntityGraph(attributePaths = "aisle")
    List<Shelf> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "aisle")
    Page<Shelf> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "aisle")
    Optional<Shelf> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = "aisle")
    List<Shelf> findAllByAisleIdAndActiveTrue(UUID aisleId);

    boolean existsByAisleIdAndActiveTrue(UUID aisleId);
}
