package com.example.warehouse_proj.shelf;

import com.example.warehouse_proj.aisle.Aisle;
import com.example.warehouse_proj.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shelves")
public class Shelf extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aisle_id", nullable = false)
    private Aisle aisle;

    @Column(nullable = false, length = 100)
    private String name;

    protected Shelf() {
    }

    public Shelf(Aisle aisle, String name) {
        this.aisle = aisle;
        this.name = name;
    }

    public Aisle getAisle() {
        return aisle;
    }

    public void setAisle(Aisle aisle) {
        this.aisle = aisle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
