package com.example.warehouse_proj.common;

import com.example.warehouse_proj.report.PdfGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: no Spring context and no database. */
class PaginationAndPdfTests {

    private final PaginationService pagination = new PaginationService();
    private final PdfGeneratorService pdf = new PdfGeneratorService();

    @Test
    void defaultsToFirstPageNewestFirstWithIdTieBreaker() {
        Pageable p = pagination.toPageable(new PageQuery(null, null, null, null), Set.of("name"));

        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(PaginationService.DEFAULT_SIZE);
        assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(p.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void clampsSizeAndAcceptsWhitelistedSortField() {
        Pageable p = pagination.toPageable(new PageQuery(2, 10_000, "name", "asc"), Set.of("name"));

        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getPageSize()).isEqualTo(PaginationService.MAX_SIZE);
        assertThat(p.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rejectsBadPagingAndNonWhitelistedSort() {
        Set<String> allowed = Set.of("name");
        assertThatThrownBy(() -> pagination.toPageable(new PageQuery(-1, null, null, null), allowed))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> pagination.toPageable(new PageQuery(0, 0, null, null), allowed))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> pagination.toPageable(new PageQuery(0, 5, "passwordHash", null), allowed))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> pagination.toPageable(new PageQuery(0, 5, "name", "sideways"), allowed))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void directionIsCaseInsensitive() {
        assertThat(pagination.toPageable(new PageQuery(0, 5, "name", "DESC"), Set.of("name"))
                .getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pagination.toPageable(new PageQuery(0, 5, "name", "Asc"), Set.of("name"))
                .getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void generatesAPdfAcrossSeveralPages() {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            rows.add(Arrays.asList("Name " + i, null, "row " + i + "@example.com"));
        }

        byte[] bytes = pdf.generateTable("Test report", List.of("Name", "Phone", "Email"), rows);

        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(bytes.length).isGreaterThan(2_000);
    }

    @Test
    void generatesAPdfForAnEmptyList() {
        byte[] bytes = pdf.generateTable("Empty", List.of("A", "B"), List.of());

        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void rejectsRowsWithTheWrongNumberOfCells() {
        assertThatThrownBy(() -> pdf.generateTable("Bad", List.of("A", "B"), List.of(List.of("only one"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
