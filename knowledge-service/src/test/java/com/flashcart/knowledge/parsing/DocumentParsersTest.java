package com.flashcart.knowledge.parsing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentParsersTest {

    private final DocumentParsers parsers =
            new DocumentParsers(List.of(new PdfDocumentParser(), new DocxDocumentParser()));

    @Test
    void pdfIsSplitPerPageAndBlankPagesAreSkipped() throws IOException {
        byte[] pdf = pdfWithPages("Earbuds carry a 12 month warranty.", "", "Returns are accepted within 7 days.");

        ParsedDocument parsed = parsers.parse("policy.pdf", pdf);

        assertThat(parsed.pageCount()).isEqualTo(3);
        assertThat(parsed.sections()).extracting(ParsedSection::pageNumber).containsExactly(1, 3);
        assertThat(parsed.sections().get(0).text()).contains("12 month warranty");
        assertThat(parsed.sections().get(1).text()).contains("7 days");
    }

    @Test
    void docxIsSplitPerHeadingAndKeepsTableRows() throws IOException {
        byte[] docx = docxWithSections();

        ParsedDocument parsed = parsers.parse("faq.docx", docx);

        assertThat(parsed.sections()).extracting(ParsedSection::heading)
                .containsExactly("Warranty", "Shipping");
        assertThat(parsed.sections().get(0).text()).contains("12 months");
        assertThat(parsed.sections().get(1).text()).contains("Metro cities | 2-3 days");
    }

    @Test
    void fileThatOnlyPretendsToBeAPdfIsRejected() {
        byte[] notAPdf = "just some text".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parsers.parse("invoice.pdf", notAPdf))
                .isInstanceOf(UnsupportedDocumentException.class);
    }

    @Test
    void unsupportedFileTypeIsRejected() {
        assertThatThrownBy(() -> parsers.parse("notes.txt", "hello".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(UnsupportedDocumentException.class)
                .hasMessageContaining("Only PDF and DOCX");
    }

    private static byte[] pdfWithPages(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (!text.isEmpty()) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(font, 12);
                        stream.newLineAtOffset(72, 700);
                        stream.showText(text);
                        stream.endText();
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] docxWithSections() throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            heading(document, "Warranty");
            paragraph(document, "All earbuds are covered for 12 months from delivery.");
            heading(document, "Shipping");
            paragraph(document, "Delivery times by area:");
            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Metro cities");
            table.getRow(0).getCell(1).setText("2-3 days");
            table.getRow(1).getCell(0).setText("Other areas");
            table.getRow(1).getCell(1).setText("4-6 days");
            document.write(out);
            return out.toByteArray();
        }
    }

    private static void heading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle("Heading1");
        paragraph.createRun().setText(text);
    }

    private static void paragraph(XWPFDocument document, String text) {
        document.createParagraph().createRun().setText(text);
    }
}