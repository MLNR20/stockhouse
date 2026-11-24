package com.example.warehouse_proj.aisle;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;

import java.util.List;
import java.util.UUID;

/** Aisle use cases. Implemented by {@link AisleServiceImpl}. */
public interface AisleService {

    /** All active aisles, optionally only those in the given warehouse (null = all). */
    List<AisleDtos.Response> list(UUID warehouseId);

    PageResponse<AisleDtos.Response> page(PageQuery query);

    AisleDtos.Response get(UUID id);

    AisleDtos.Response create(AisleDtos.Request request);

    AisleDtos.Response update(UUID id, AisleDtos.Request request);

    /** Soft delete; refused (409) while the aisle still has active shelves. */
    void delete(UUID id);
}
