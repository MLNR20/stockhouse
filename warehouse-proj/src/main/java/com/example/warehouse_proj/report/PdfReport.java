package com.example.warehouse_proj.report;

import java.util.List;

/**
 * One kind of PDF report. To add a report, implement this as a Spring @Component: PdfReportFactory
 * picks it up automatically and it becomes available at /api/reports/{type}.pdf.
 */
public interface PdfReport {

    /** URL key and file name, lower case, e.g. "customers". Must be unique across reports. */
    String type();

    /** Heading printed at the top of the PDF. */
    String title();

    List<String> headers();

    /** One list of cell texts per row, each as long as headers(). */
    List<List<String>> rows();
}
