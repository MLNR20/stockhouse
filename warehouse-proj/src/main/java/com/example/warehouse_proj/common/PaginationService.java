package com.example.warehouse_proj.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.function.Function;

/**
 * Server-side pagination for any BaseRepository. Validates page/size, clamps size to MAX_SIZE and
 * only sorts by a per-entity whitelist, so clients cannot sort by arbitrary or lazy properties.
 * Call it from inside a @Transactional method when the mapper touches lazy associations.
 */
@Service
public class PaginationService {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /** Sort fields every BaseEntity supports; entity-specific ones are passed in per call. */
    public static final Set<String> BASE_SORT_FIELDS = Set.of("createdAt", "updatedAt");

    /** Active rows of {@code repository}, paged, sorted and mapped to a response DTO. */
    public <T extends BaseEntity, R> PageResponse<R> page(
            BaseRepository<T> repository,
            PageQuery query,
            Set<String> allowedSortFields,
            Function<T, R> mapper) {
        Page<T> result = repository.findAllByActiveTrue(toPageable(query, allowedSortFields));
        return PageResponse.of(result, mapper);
    }

    private static Sort.Direction parseDirection(String value) {
        if (value == null || value.isBlank()) {
            return Sort.Direction.DESC;
        }
        try {
            return Sort.Direction.fromString(value.trim()); // case-insensitive
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be asc or desc");
        }
    }

    public Pageable toPageable(PageQuery query, Set<String> allowedSortFields) {
        int page = query.page() == null ? 0 : query.page();
        int size = query.size() == null ? DEFAULT_SIZE : query.size();
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be >= 1");
        }
        size = Math.min(size, MAX_SIZE);

        String sortBy = query.sortBy() == null || query.sortBy().isBlank()
                ? DEFAULT_SORT_FIELD
                : query.sortBy().trim();
        if (!BASE_SORT_FIELDS.contains(sortBy) && !allowedSortFields.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot sort by '" + sortBy + "'");
        }
        Sort.Direction direction = parseDirection(query.direction());

        // id as tie-breaker keeps page boundaries stable when the sort column has duplicates.
        return PageRequest.of(page, size, Sort.by(direction, sortBy).and(Sort.by(Sort.Direction.ASC, "id")));
    }
}
