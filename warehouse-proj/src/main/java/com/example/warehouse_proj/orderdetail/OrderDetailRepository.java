package com.example.warehouse_proj.orderdetail;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDetailRepository extends BaseRepository<OrderDetail> {

    @Override
    @EntityGraph(attributePaths = {"customerOrder", "product", "howSold"})
    List<OrderDetail> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = {"customerOrder", "product", "howSold"})
    Page<OrderDetail> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customerOrder", "product", "howSold"})
    Optional<OrderDetail> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"customerOrder", "product", "howSold"})
    List<OrderDetail> findAllByCustomerOrderIdAndActiveTrue(UUID orderId);

    /** One line per product per order. */
    boolean existsByCustomerOrderIdAndProductIdAndActiveTrue(UUID orderId, UUID productId);

    boolean existsByCustomerOrderIdAndProductIdAndActiveTrueAndIdNot(UUID orderId, UUID productId, UUID id);
}
