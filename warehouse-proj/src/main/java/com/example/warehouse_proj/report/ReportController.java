package com.example.warehouse_proj.report;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/** PDF exports of the active records of each list. The report itself comes from PdfReportFactory. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PdfReportFactory factory;
    private final PdfGeneratorService pdf;

    public ReportController(PdfReportFactory factory, PdfGeneratorService pdf) {
        this.factory = factory;
        this.pdf = pdf;
    }

    /** The available report types, e.g. ["customers", "suppliers", "products", "orders"]. */
    @GetMapping
    public Set<String> types() {
        return factory.types();
    }

    /**
     * GET /api/reports/orders.pdf downloads the PDF; add {@code ?inline=true} to show it in the
     * browser's own PDF viewer instead, which is the way to preview and print it.
     */
    @GetMapping("/{type}.pdf")
    public ResponseEntity<byte[]> report(@PathVariable String type, @RequestParam(defaultValue = "false") boolean inline) {
        PdfReport report = factory.create(type);
        byte[] content = pdf.generateTable(report.title(), report.headers(), report.rows());

        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(report.type() + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }
}
