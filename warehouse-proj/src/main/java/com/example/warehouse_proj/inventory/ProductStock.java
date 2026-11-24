package com.example.warehouse_proj.inventory;

import java.util.UUID;

/** Total base quantity on hand for one product across its active inventory records (JPQL constructor expression). */
public record ProductStock(UUID productId, Long quantity) {
}
