package com.example.warehouse_proj.report;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for PDF reports: hands out the PdfReport for a type name ("orders" -> OrdersPdfReport).
 * Spring injects every PdfReport bean, so adding a report is just adding a class: no factory
 * edit and no new controller method.
 */
@Component
public class PdfReportFactory {

    private final Map<String, PdfReport> reports;

    public PdfReportFactory(List<PdfReport> available) {
        this.reports = Collections.unmodifiableMap(available.stream().collect(Collectors.toMap(
                PdfReport::type,
                Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException("Two PDF reports share the type '" + a.type() + "'");
                },
                LinkedHashMap::new)));
    }

    /** @throws ResponseStatusException 404 if there is no report of that type */
    public PdfReport create(String type) {
        PdfReport report = reports.get(type.toLowerCase(Locale.ROOT));
        if (report == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + type);
        }
        return report;
    }

    /** The report types that can be requested, in a stable order. */
    public Set<String> types() {
        return reports.keySet();
    }
}
