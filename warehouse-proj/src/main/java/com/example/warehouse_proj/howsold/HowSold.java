package com.example.warehouse_proj.howsold;

import com.example.warehouse_proj.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A unit a product is sold in (e.g. Each, Case, Pallet). The how-sold id is the BaseEntity id. */
@Entity
@Table(name = "how_sold")
public class HowSold extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    protected HowSold() {
    }

    public HowSold(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
