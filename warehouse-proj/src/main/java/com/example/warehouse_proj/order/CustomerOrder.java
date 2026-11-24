package com.example.warehouse_proj.order;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Named CustomerOrder in code because ORDER is a reserved SQL word; the table is "orders". */
@Entity
@Table(name = "orders")
public class CustomerOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "target_delivery_date", nullable = false)
    private LocalDate targetDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    protected CustomerOrder() {
    }

    public CustomerOrder(Customer customer, BigDecimal totalPrice, LocalDate targetDeliveryDate, OrderStatus status) {
        this.customer = customer;
        this.totalPrice = totalPrice;
        this.targetDeliveryDate = targetDeliveryDate;
        this.status = status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDate getTargetDeliveryDate() {
        return targetDeliveryDate;
    }

    public void setTargetDeliveryDate(LocalDate targetDeliveryDate) {
        this.targetDeliveryDate = targetDeliveryDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
