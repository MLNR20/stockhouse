package com.example.warehouse_proj.report;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: no Spring context and no database. */
class PdfReportFactoryTests {

    private static PdfReport report(String type) {
        return new PdfReport() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public String title() {
                return type;
            }

            @Override
            public List<String> headers() {
                return List.of("A");
            }

            @Override
            public List<List<String>> rows() {
                return List.of();
            }
        };
    }

    @Test
    void createsTheReportForATypeCaseInsensitively() {
        PdfReport orders = report("orders");
        PdfReportFactory factory = new PdfReportFactory(List.of(report("customers"), orders));

        assertThat(factory.create("orders")).isSameAs(orders);
        assertThat(factory.create("ORDERS")).isSameAs(orders);
        assertThat(factory.types()).containsExactly("customers", "orders");
    }

    @Test
    void unknownTypeIsNotFound() {
        PdfReportFactory factory = new PdfReportFactory(List.of(report("orders")));

        assertThatThrownBy(() -> factory.create("payroll"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void duplicateTypesFailAtStartup() {
        assertThatThrownBy(() -> new PdfReportFactory(List.of(report("orders"), report("orders"))))
                .isInstanceOf(IllegalStateException.class);
    }
}
