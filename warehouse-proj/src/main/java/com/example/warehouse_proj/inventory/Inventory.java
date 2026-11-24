package com.example.warehouse_proj.inventory;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** The inventory id is the BaseEntity id. */
@Entity
@Table(name = "inventory")
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryStatus status;

    /** Stock on hand, counted in the base unit (the smallest unit, e.g. "each"). */
    @Column(name = "base_quantity", nullable = false)
    private int baseQuantity;

    /** At or below this much on hand counts as running low. */
    @Column(name = "minimum_threshold", nullable = false)
    private int minimumThreshold;

    /** The unit the product is sold in (case, pack...); optional. See HowSoldConversion for how units relate. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "how_sold_id")
    private HowSold howSold;

    protected Inventory() {
    }

    public Inventory(Product product, InventoryStatus status) {
        this.product = product;
        this.status = status;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public int getBaseQuantity() {
        return baseQuantity;
    }

    public void setBaseQuantity(int baseQuantity) {
        this.baseQuantity = baseQuantity;
    }

    public int getMinimumThreshold() {
        return minimumThreshold;
    }

    public void setMinimumThreshold(int minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

    public HowSold getHowSold() {
        return howSold;
    }

    public void setHowSold(HowSold howSold) {
        this.howSold = howSold;
    }
}
