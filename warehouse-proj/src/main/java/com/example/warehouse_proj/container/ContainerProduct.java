package com.example.warehouse_proj.container;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A product held in a container, with its quantity. Removing it sets isActive = false. */
@Entity
@Table(name = "container_products")
public class ContainerProduct extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    protected ContainerProduct() {
    }

    public ContainerProduct(Container container, Product product, int quantity) {
        this.container = container;
        this.product = product;
        this.quantity = quantity;
    }

    public Container getContainer() {
        return container;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
