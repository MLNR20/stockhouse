package com.example.warehouse_proj.order;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.warehouse_proj.analytics.DayCount;
import com.example.warehouse_proj.analytics.OrderStatusStat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends BaseRepository<CustomerOrder> {

    // The response only needs the customer id, but fetching avoids lazy-proxy surprises.
    @Override
    @EntityGraph(attributePaths = "customer")
    List<CustomerOrder> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "customer")
    Page<CustomerOrder> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "customer")
    Optional<CustomerOrder> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = "customer")
    List<CustomerOrder> findAllByCustomerIdAndActiveTrue(UUID customerId);

    // ---- analytics -------------------------------------------------------------------------

    @Query("""
            select new com.example.warehouse_proj.analytics.OrderStatusStat(o.status, count(o), sum(o.totalPrice))
            from CustomerOrder o
            where o.active = true
            group by o.status""")
    List<OrderStatusStat> statusStats();

    @Query("""
            select new com.example.warehouse_proj.analytics.DayCount(o.targetDeliveryDate, count(o))
            from CustomerOrder o
            where o.active = true
              and o.status not in :closed
              and o.targetDeliveryDate between :from and :to
            group by o.targetDeliveryDate
            order by o.targetDeliveryDate""")
    List<DayCount> openDeliveriesBetween(
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("closed") Collection<OrderStatus> closed);

    long countByActiveTrueAndTargetDeliveryDateBeforeAndStatusNotIn(LocalDate date, Collection<OrderStatus> closed);
}
