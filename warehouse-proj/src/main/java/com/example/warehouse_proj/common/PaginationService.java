package com.example.warehouse_proj.common;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
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

    /** Longer search texts are cut to this many words. */
    private static final int MAX_SEARCH_WORDS = 6;

    /** Active rows of {@code repository}, searched, paged, sorted and mapped to a response DTO. */
    public <T extends BaseEntity, R> PageResponse<R> page(
            BaseRepository<T> repository,
            PageQuery query,
            Set<String> allowedSortFields,
            List<String> searchFields,
            Function<T, R> mapper) {
        return PageResponse.of(find(repository, query, allowedSortFields, searchFields), mapper);
    }

    /**
     * One page of active rows. With a search text, only rows where every word appears (ignoring case) in at least
     * one of {@code searchFields}; a field may reach into a related record, e.g. "supplier.supplierName".
     */
    public <T extends BaseEntity> Page<T> find(
            BaseRepository<T> repository,
            PageQuery query,
            Set<String> allowedSortFields,
            List<String> searchFields) {
        Pageable pageable = toPageable(query, allowedSortFields);
        List<String> words = searchWords(query.search());
        if (words.isEmpty() || searchFields.isEmpty()) {
            return repository.findAllByActiveTrue(pageable);
        }
        Specification<T> matching = (root, criteria, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isTrue(root.get("active")));
            // One left join per relation, shared by all words and fields.
            Map<String, From<?, ?>> joins = new HashMap<>();
            for (String word : words) {
                String like = "%" + escapeLike(word) + "%";
                Predicate[] any = searchFields.stream()
                        .map(field -> cb.like(cb.lower(asText(cb, path(root, field, joins))), like, '!'))
                        .toArray(Predicate[]::new);
                all.add(cb.or(any));
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
        return repository.findAll(matching, pageable);
    }

    static List<String> searchWords(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }
        return Arrays.stream(search.trim().toLowerCase().split("\\s+")).limit(MAX_SEARCH_WORDS).toList();
    }

    /** So that a typed % or _ is looked for literally. */
    static String escapeLike(String word) {
        return word.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * The value as text in the database. Ids, dates, numbers and enums need a real SQL cast (JPA's as() only
     * changes the Java type, and the database has no lower(uuid) or lower(date)).
     */
    @SuppressWarnings("unchecked")
    private static Expression<String> asText(CriteriaBuilder cb, Expression<?> value) {
        if (value.getJavaType() == String.class) {
            return (Expression<String>) value;
        }
        return ((HibernateCriteriaBuilder) cb).cast((JpaExpression<?>) value, String.class);
    }

    private static Expression<?> path(From<?, ?> root, String field, Map<String, From<?, ?>> joins) {
        String[] parts = field.split("\\.");
        From<?, ?> from = root;
        StringBuilder walked = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            walked.append(parts[i]).append('.');
            From<?, ?> parent = from;
            String part = parts[i];
            from = joins.computeIfAbsent(walked.toString(), key -> parent.join(part, JoinType.LEFT));
        }
        return from.get(parts[parts.length - 1]);
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
