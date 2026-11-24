package com.example.warehouse_proj.supplier;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;

import java.util.List;
import java.util.UUID;

/** Supplier use cases. Implemented by {@link SupplierServiceImpl}. */
public interface SupplierService {

    List<SupplierDtos.Response> list();

    PageResponse<SupplierDtos.Response> page(PageQuery query);

    SupplierDtos.Response get(UUID id);

    SupplierDtos.Response create(SupplierDtos.Request request);

    SupplierDtos.Response update(UUID id, SupplierDtos.Request request);

    /** Soft delete: the supplier and its product links get isActive = false. */
    void delete(UUID id);

    /** Links a product to the supplier, reactivating a previously removed link. */
    void addProduct(UUID supplierId, UUID productId);

    /** Soft-deletes the link; a no-op if the product was not linked. */
    void removeProduct(UUID supplierId, UUID productId);
}
