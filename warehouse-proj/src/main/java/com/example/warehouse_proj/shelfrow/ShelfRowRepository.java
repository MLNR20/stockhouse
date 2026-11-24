package com.example.warehouse_proj.shelfrow;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShelfRowRepository extends BaseRepository<ShelfRow> {

    @Override
    @EntityGraph(attributePaths = "shelf")
    List<ShelfRow> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "shelf")
    Page<ShelfRow> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "shelf")
    Optional<ShelfRow> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = "shelf")
    List<ShelfRow> findAllByShelfIdAndActiveTrue(UUID shelfId);

    boolean existsByShelfIdAndActiveTrue(UUID shelfId);
}
