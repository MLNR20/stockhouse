package com.example.warehouse_proj.common;

import com.example.warehouse_proj.category.CategoryDtos;
import com.example.warehouse_proj.category.CategoryService;
import com.example.warehouse_proj.howsold.HowSoldDtos;
import com.example.warehouse_proj.howsold.HowSoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * One active category / unit per name, checked against the real database. Each test runs in a transaction that is
 * rolled back, so nothing it creates is kept.
 */
@SpringBootTest
@Transactional
class ActiveUniquenessTests {

    @Autowired
    private CategoryService categories;

    @Autowired
    private HowSoldService units;

    private static String freshName() {
        return "Zz Test " + UUID.randomUUID().toString().substring(0, 12);
    }

    private static void assertConflict(Runnable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(409, e.getStatusCode().value());
    }

    /** The same rules for both kinds of record. */
    private static void checkRules(
            Function<String, UUID> create, BiConsumer<UUID, String> rename, Consumer<UUID> delete) {
        String name = freshName();
        UUID first = create.apply(name);

        assertConflict(() -> create.apply(name));
        assertConflict(() -> create.apply("  " + name.toUpperCase() + "  "));

        // Saving a record under its own name is not a clash, but taking another's name is.
        assertDoesNotThrow(() -> rename.accept(first, name.toLowerCase()));
        UUID other = create.apply(freshName());
        assertConflict(() -> rename.accept(other, name));

        // Deleting is soft, and frees the name.
        delete.accept(first);
        UUID replacement = create.apply(name);
        assertNotEquals(first, replacement);
        assertConflict(() -> create.apply(name));
    }

    @Test
    void categoriesAllowOneActiveRecordPerName() {
        checkRules(
                name -> categories.create(new CategoryDtos.Request(name, null)).id(),
                (id, name) -> categories.update(id, new CategoryDtos.Request(name, null)),
                categories::delete);
    }

    @Test
    void unitsOfMeasureAllowOneActiveRecordPerName() {
        checkRules(
                name -> units.create(new HowSoldDtos.Request(name)).id(),
                (id, name) -> units.update(id, new HowSoldDtos.Request(name)),
                units::delete);
    }

    @Test
    void deletedCategoriesStayInTheDatabase() {
        UUID id = categories.create(new CategoryDtos.Request(freshName(), null)).id();

        categories.delete(id);

        assertEquals(false, categories.list(true).stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow().active());
    }
}
