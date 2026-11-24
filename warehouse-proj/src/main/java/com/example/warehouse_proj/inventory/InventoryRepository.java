package com.example.warehouse_proj.inventory;

import com.example.warehouse_proj.analytics.InventoryStatusStat;
import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends BaseRepository<Inventory> {

    @Override
    @EntityGraph(attributePaths = {"product", "howSold"})
    List<Inventory> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = {"product", "howSold"})
    Page<Inventory> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"product", "howSold"})
    Optional<Inventory> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"product", "howSold"})
    List<Inventory> findAllByProductIdAndActiveTrue(UUID productId);

    @Query("""
            select new com.example.warehouse_proj.analytics.InventoryStatusStat(i.status, count(i))
            from Inventory i
            where i.active = true
            group by i.status""")
    List<InventoryStatusStat> statusStats();

    /** How much of each product is on hand (base quantity summed over its active inventory records). */
    @Query("""
            select new com.example.warehouse_proj.inventory.ProductStock(i.product.id, sum(i.baseQuantity))
            from Inventory i
            where i.active = true and i.product.id in :productIds
            group by i.product.id""")
    List<ProductStock> stockFor(@Param("productIds") Collection<UUID> productIds);
}
