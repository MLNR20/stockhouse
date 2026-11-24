package com.example.warehouse_proj.shelfrow;

import com.example.warehouse_proj.common.BaseEntity;
import com.example.warehouse_proj.shelf.Shelf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A row on a shelf. Named ShelfRow in code because ROW is a reserved SQL word; the table is shelf_rows. */
@Entity
@Table(name = "shelf_rows")
public class ShelfRow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private Shelf shelf;

    @Column(nullable = false, length = 100)
    private String name;

    protected ShelfRow() {
    }

    public ShelfRow(Shelf shelf, String name) {
        this.shelf = shelf;
        this.name = name;
    }

    public Shelf getShelf() {
        return shelf;
    }

    public void setShelf(Shelf shelf) {
        this.shelf = shelf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
