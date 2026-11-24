package com.example.warehouse_proj.product;

import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;

import java.util.List;
import java.util.UUID;

/** Product use cases. Implemented by {@link ProductServiceImpl}. */
public interface ProductService {

    /** All active products, optionally only those in the given category (null = all). */
    List<ProductDtos.Response> list(UUID categoryId);

    PageResponse<ProductDtos.Response> page(PageQuery query);

    ProductDtos.Response get(UUID id);

    ProductDtos.Response create(ProductDtos.Request request);

    ProductDtos.Response update(UUID id, ProductDtos.Request request);

    /** Soft delete: the product and its supplier links get isActive = false. */
    void delete(UUID id);
}
