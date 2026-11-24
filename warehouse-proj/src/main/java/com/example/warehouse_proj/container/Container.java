package com.example.warehouse_proj.container;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.shelfrow.ShelfRow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A container (bin, tote, box...) sitting in a row. Its products are ContainerProduct rows. */
@Entity
@Table(name = "containers")
public class Container extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_row_id", nullable = false)
    private ShelfRow shelfRow;

    @Column(nullable = false, length = 100)
    private String name;

    protected Container() {
    }

    public Container(ShelfRow shelfRow, String name) {
        this.shelfRow = shelfRow;
        this.name = name;
    }

    public ShelfRow getShelfRow() {
        return shelfRow;
    }

    public void setShelfRow(ShelfRow shelfRow) {
        this.shelfRow = shelfRow;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
