package com.example.warehouse_proj.common;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Only one active record may hold this value at a time." A record can be saved when no other record has the
 * value, or when every record that has it is soft-deleted (is_active = false).
 *
 * <p>Text is compared ignoring case and surrounding spaces, so " case " clashes with "Case". Works for any entity
 * with a {@link BaseRepository}:
 *
 * <pre>{@code
 * // adding
 * ActiveUniqueness.require(categories, "name", name, null, "A category with that name already exists");
 * // renaming: the record being edited does not clash with itself
 * ActiveUniqueness.require(categories, "name", name, id, "A category with that name already exists");
 * // unique only together, e.g. one aisle name per warehouse
 * ActiveUniqueness.require(aisles, Map.of("name", name, "warehouse.id", warehouseId), id, "...");
 * }</pre>
 *
 * <p>Call it before changing the entity: the check queries the database, which would first flush a pending change.
 * This check gives a clear message; a partial unique index (WHERE is_active) should back it, so that two requests
 * arriving at the same moment cannot both get through.
 */
public final class ActiveUniqueness {

    private ActiveUniqueness() {
    }

    /** Refuses (409) if another active record already has {@code value} in {@code field}. */
    public static <T extends BaseEntity> void require(
            BaseRepository<T> repository, String field, Object value, UUID excludeId, String message) {
        require(repository, Map.of(field, value), excludeId, message);
    }

    /** Refuses (409) if another active record matches all of {@code values} (field name to value). */
    public static <T extends BaseEntity> void require(
            BaseRepository<T> repository, Map<String, ?> values, UUID excludeId, String message) {
        if (isTaken(repository, values, excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    /**
     * Whether an active record other than {@code excludeId} (null when adding) already has all of these values.
     * A field may reach into a related record, e.g. "warehouse.id".
     */
    public static <T extends BaseEntity> boolean isTaken(
            BaseRepository<T> repository, Map<String, ?> values, UUID excludeId) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one field is needed to decide uniqueness");
        }
        Specification<T> clash = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isTrue(root.get("active")));
            if (excludeId != null) {
                all.add(cb.notEqual(root.get("id"), excludeId));
            }
            values.forEach((field, value) -> {
                Path<?> path = root;
                for (String part : field.split("\\.")) {
                    path = path.get(part);
                }
                if (value instanceof String text) {
                    all.add(cb.equal(cb.lower(cb.trim(path.as(String.class))), text.trim().toLowerCase()));
                } else {
                    all.add(cb.equal(path, value));
                }
            });
            return cb.and(all.toArray(Predicate[]::new));
        };
        return repository.exists(clash);
    }
}
