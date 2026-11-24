package com.example.warehouse_proj.reorder;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReorderRepository extends BaseRepository<Reorder> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "product"})
    List<Reorder> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = {"supplier", "product"})
    Page<Reorder> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"supplier", "product"})
    Optional<Reorder> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"supplier", "product"})
    List<Reorder> findAllBySupplierIdAndActiveTrue(UUID supplierId);

    @EntityGraph(attributePaths = {"supplier", "product"})
    List<Reorder> findAllByProductIdAndActiveTrue(UUID productId);

    // Used by the supplier / product delete guards.
    boolean existsBySupplierIdAndActiveTrueAndStatusIn(UUID supplierId, Collection<ReorderStatus> statuses);

    boolean existsByProductIdAndActiveTrueAndStatusIn(UUID productId, Collection<ReorderStatus> statuses);
}
