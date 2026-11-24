package com.example.warehouse_proj.container;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContainerRepository extends BaseRepository<Container> {

    @Override
    @EntityGraph(attributePaths = "shelfRow")
    List<Container> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "shelfRow")
    Page<Container> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "shelfRow")
    Optional<Container> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = "shelfRow")
    List<Container> findAllByShelfRowIdAndActiveTrue(UUID shelfRowId);

    boolean existsByShelfRowIdAndActiveTrue(UUID shelfRowId);
}
