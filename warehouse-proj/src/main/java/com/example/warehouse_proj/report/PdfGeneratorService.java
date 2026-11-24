package com.example.warehouse_proj.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generic PDF generator: a titled, paginated table. Build the headers and rows from any DTO list
 * and hand them in; the caller decides the columns, this class only does the layout.
 */
@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
    private static final Color HEADER_BACKGROUND = new Color(0x2F, 0x3B, 0x52);
    private static final Color STRIPE_BACKGROUND = new Color(0xF2, 0xF4, 0xF7);

    /**
     * @param title   shown at the top of the first page
     * @param headers column headings
     * @param rows    one list of cell texts per row; each must have headers.size() cells (null = blank)
     */
    public byte[] generateTable(String title, List<String> headers, List<List<String>> rows) {
        for (List<String> row : rows) {
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException(
                        "Row has " + row.size() + " cells but there are " + headers.size() + " headers");
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 48);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumbers());
            document.open();

            document.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            Paragraph stamp = new Paragraph(
                    "Generated " + ZonedDateTime.now(ZoneId.systemDefault()).format(STAMP)
                            + "  |  " + rows.size() + (rows.size() == 1 ? " record" : " records"),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY));
            stamp.setSpacingAfter(12);
            document.add(stamp);

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1); // repeats the header on every page

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(HEADER_BACKGROUND);
                cell.setPadding(6);
                table.addCell(cell);
            }

            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (int r = 0; r < rows.size(); r++) {
                for (String text : rows.get(r)) {
                    PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, bodyFont));
                    cell.setPadding(5);
                    cell.setBorderColor(Color.LIGHT_GRAY);
                    if (r % 2 == 1) {
                        cell.setBackgroundColor(STRIPE_BACKGROUND);
                    }
                    table.addCell(cell);
                }
            }
            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No records", bodyFont));
                empty.setColspan(headers.size());
                empty.setPadding(10);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(empty);
            }
            document.add(table);
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    /** Footer with "Page N" centred on every page. */
    private static final class PageNumbers extends PdfPageEventHelper {

        private final Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    new Phrase("Page " + writer.getPageNumber(), font),
                    (page.getLeft() + page.getRight()) / 2,
                    document.bottom() - 18,
                    0);
        }
    }
}
