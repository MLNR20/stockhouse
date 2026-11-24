package com.example.warehouse_proj.shipment;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.employee.WarehouseEmployee;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.truck.Truck;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * One delivery run for part or all of an order. An order has many shipments, a truck carries many
 * shipments, and an employee (optional) handles many shipments.
 */
@Entity
@Table(name = "shipments")
public class Shipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder customerOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private WarehouseEmployee employee;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    protected Shipment() {
    }

    public Shipment(
            CustomerOrder customerOrder,
            Truck truck,
            WarehouseEmployee employee,
            LocalDate scheduledDate,
            ShipmentStatus status) {
        this.customerOrder = customerOrder;
        this.truck = truck;
        this.employee = employee;
        this.scheduledDate = scheduledDate;
        this.status = status;
    }

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }

    public Truck getTruck() {
        return truck;
    }

    public void setTruck(Truck truck) {
        this.truck = truck;
    }

    public WarehouseEmployee getEmployee() {
        return employee;
    }

    public void setEmployee(WarehouseEmployee employee) {
        this.employee = employee;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }
}
