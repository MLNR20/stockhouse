package com.example.warehouse_proj.supplier;

import com.example.warehouse_proj.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Products of a supplier are linked through SupplierProduct (soft-deleted link rows). */
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 256)
    private String email;

    protected Supplier() {
    }

    public Supplier(String supplierName, String phoneNumber, String email) {
        this.supplierName = supplierName;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
