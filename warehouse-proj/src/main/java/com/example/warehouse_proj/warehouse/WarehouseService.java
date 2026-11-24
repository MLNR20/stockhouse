package com.example.warehouse_proj.warehouse;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;

import java.util.List;
import java.util.UUID;

/** Warehouse use cases. Implemented by {@link WarehouseServiceImpl}. */
public interface WarehouseService {

    List<WarehouseDtos.Response> list();

    PageResponse<WarehouseDtos.Response> page(PageQuery query);

    WarehouseDtos.Response get(UUID id);

    WarehouseDtos.Response create(WarehouseDtos.Request request);

    WarehouseDtos.Response update(UUID id, WarehouseDtos.Request request);

    /** Soft delete; refused (409) while the warehouse still has active aisles. */
    void delete(UUID id);
}
