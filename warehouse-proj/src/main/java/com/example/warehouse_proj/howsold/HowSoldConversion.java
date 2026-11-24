package com.example.warehouse_proj.howsold;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 1 parent unit = unitConversion child units (e.g. parent Case, child Each, unitConversion 12), for one
 * product, or for every product when {@code product} is null.
 */
@Entity
@Table(name = "how_sold_conversions")
public class HowSoldConversion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_how_sold_id", nullable = false)
    private HowSold parentHowSold;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_how_sold_id", nullable = false)
    private HowSold childHowSold;

    @Column(name = "unit_conversion", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitConversion;

    /** The product this conversion is for; null means it applies to all products. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    protected HowSoldConversion() {
    }

    public HowSoldConversion(HowSold parentHowSold, HowSold childHowSold, BigDecimal unitConversion) {
        this.parentHowSold = parentHowSold;
        this.childHowSold = childHowSold;
        this.unitConversion = unitConversion;
    }

    public HowSold getParentHowSold() {
        return parentHowSold;
    }

    public void setParentHowSold(HowSold parentHowSold) {
        this.parentHowSold = parentHowSold;
    }

    public HowSold getChildHowSold() {
        return childHowSold;
    }

    public void setChildHowSold(HowSold childHowSold) {
        this.childHowSold = childHowSold;
    }

    public BigDecimal getUnitConversion() {
        return unitConversion;
    }

    public void setUnitConversion(BigDecimal unitConversion) {
        this.unitConversion = unitConversion;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
