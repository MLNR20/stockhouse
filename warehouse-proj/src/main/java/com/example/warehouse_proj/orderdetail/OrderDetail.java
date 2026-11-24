package com.example.warehouse_proj.orderdetail;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One line of an order: a product, how many (counted in the "how sold" unit), and the price it is sold at
 * (which can differ from the base price).
 */
@Entity
@Table(name = "order_details")
public class OrderDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder customerOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "sales_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salesPrice;

    /** The unit the quantity is counted in; null means the base unit. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "how_sold_id")
    private HowSold howSold;

    protected OrderDetail() {
    }

    public OrderDetail(CustomerOrder customerOrder, Product product, int quantity, BigDecimal salesPrice) {
        this.customerOrder = customerOrder;
        this.product = product;
        this.quantity = quantity;
        this.salesPrice = salesPrice;
    }

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
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

    public BigDecimal getSalesPrice() {
        return salesPrice;
    }

    public void setSalesPrice(BigDecimal salesPrice) {
        this.salesPrice = salesPrice;
    }

    public HowSold getHowSold() {
        return howSold;
    }

    public void setHowSold(HowSold howSold) {
        this.howSold = howSold;
    }
}
