package com.example.warehouse_proj.reorder;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.supplier.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** A request to a supplier for more stock of a product. */
@Entity
@Table(name = "reorders")
public class Reorder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    /** The unit the quantity is counted in (Each, Case...); null when not set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "how_sold_id")
    private HowSold howSold;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReorderStatus status;

    protected Reorder() {
    }

    public Reorder(Supplier supplier, Product product, int quantity, LocalDate expectedDate, ReorderStatus status) {
        this.supplier = supplier;
        this.product = product;
        this.quantity = quantity;
        this.expectedDate = expectedDate;
        this.status = status;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public HowSold getHowSold() {
        return howSold;
    }

    public void setHowSold(HowSold howSold) {
        this.howSold = howSold;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public ReorderStatus getStatus() {
        return status;
    }

    public void setStatus(ReorderStatus status) {
        this.status = status;
    }
}
