package com.example.warehouse_proj.container;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContainerProductRepository extends BaseRepository<ContainerProduct> {

    /** Any state (active or not), so a removed entry can be reactivated instead of duplicated. */
    Optional<ContainerProduct> findByContainerIdAndProductId(UUID containerId, UUID productId);

    @EntityGraph(attributePaths = {"container", "product"})
    List<ContainerProduct> findAllByContainerIdAndActiveTrue(UUID containerId);

    boolean existsByContainerIdAndActiveTrue(UUID containerId);
}
