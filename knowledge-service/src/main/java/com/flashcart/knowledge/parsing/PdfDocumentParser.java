package com.flashcart.knowledge.parsing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class PdfDocumentParser implements DocumentParser {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    @Override
    public boolean supports(String fileName, byte[] content) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")
                && content.length >= PDF_MAGIC.length
                && Arrays.equals(Arrays.copyOf(content, PDF_MAGIC.length), PDF_MAGIC);
    }

    @Override
    public ParsedDocument parse(String fileName, byte[] content) {
        try (PDDocument pdf = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<ParsedSection> sections = new ArrayList<>();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = TextCleaner.clean(stripper.getText(pdf));
                if (!text.isBlank()) {
                    sections.add(new ParsedSection(page, null, text));
                }
            }
            if (sections.isEmpty()) {
                throw new UnsupportedDocumentException(
                        fileName + " has no extractable text (is it a scanned image?)");
            }
            return new ParsedDocument(fileName, pdf.getNumberOfPages(), sections);
        } catch (IOException e) {
            throw new UnsupportedDocumentException("Could not read PDF " + fileName, e);
        }
    }
}