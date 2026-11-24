package com.example.warehouse_proj.supplier;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierProductRepository extends BaseRepository<SupplierProduct> {

    /** Any state (active or not), so a removed link can be reactivated instead of duplicated. */
    Optional<SupplierProduct> findBySupplierIdAndProductId(UUID supplierId, UUID productId);

    List<SupplierProduct> findAllBySupplierIdAndActiveTrue(UUID supplierId);

    List<SupplierProduct> findAllByProductIdAndActiveTrue(UUID productId);

    // Batch lookups for list/page responses; the join fetch lets us filter out inactive counterparts.
    @EntityGraph(attributePaths = "product")
    List<SupplierProduct> findAllBySupplierIdInAndActiveTrue(Collection<UUID> supplierIds);

    @EntityGraph(attributePaths = "supplier")
    List<SupplierProduct> findAllByProductIdInAndActiveTrue(Collection<UUID> productIds);
}
