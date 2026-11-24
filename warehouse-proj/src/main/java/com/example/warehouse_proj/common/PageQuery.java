package com.example.warehouse_proj.common;

/**
 * Paging request, zero-based page. Null/blank values fall back to defaults in PaginationService.
 * Typically bound from {@code ?page=0&size=20&sortBy=createdAt&direction=desc}; direction is a
 * string ("asc"/"desc", any case) because Spring's enum binding would reject lowercase.
 */
public record PageQuery(Integer page, Integer size, String sortBy, String direction) {
}
