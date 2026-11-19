package com.flashcart.knowledge.parsing;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class DocxDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName, byte[] content) {
        // A .docx is a ZIP archive, so it must start with "PK"
        return fileName.toLowerCase(Locale.ROOT).endsWith(".docx")
                && content.length >= 2 && content[0] == 'P' && content[1] == 'K';
    }

    @Override
    public ParsedDocument parse(String fileName, byte[] content) {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<ParsedSection> sections = new ArrayList<>();
            String heading = null;
            StringBuilder body = new StringBuilder();

            for (IBodyElement element : docx.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText().strip();
                    if (text.isEmpty()) {
                        continue;
                    }
                    if (isHeading(paragraph)) {
                        flush(sections, heading, body);
                        heading = text;
                    } else {
                        body.append(text).append('\n');
                    }
                } else if (element instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        body.append(row.getTableCells().stream()
                                        .map(XWPFTableCell::getText)
                                        .map(String::strip)
                                        .collect(Collectors.joining(" | ")))
                                .append('\n');
                    }
                }
            }
            flush(sections, heading, body);

            if (sections.isEmpty()) {
                throw new UnsupportedDocumentException(fileName + " contains no text");
            }
            return new ParsedDocument(fileName, null, sections);
        } catch (IOException | POIXMLException | IllegalArgumentException e) {
            throw new UnsupportedDocumentException("Could not read DOCX " + fileName, e);
        }
    }

    private static boolean isHeading(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        return style != null
                && (style.toLowerCase(Locale.ROOT).startsWith("heading") || style.equalsIgnoreCase("Title"));
    }

    private static void flush(List<ParsedSection> sections, String heading, StringBuilder body) {
        String text = TextCleaner.clean(body.toString());
        if (!text.isBlank()) {
            sections.add(new ParsedSection(null, heading, text));
        }
        body.setLength(0);
    }
}
