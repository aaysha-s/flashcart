package com.flashcart.knowledge.parsing;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentParsers {

    private final List<DocumentParser> parsers;

    public DocumentParsers(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public ParsedDocument parse(String fileName, byte[] content) {
        return parsers.stream()
                .filter(parser -> parser.supports(fileName, content))
                .findFirst()
                .orElseThrow(() -> new UnsupportedDocumentException(
                        "Only PDF and DOCX files are supported, and the content must match the extension"))
                .parse(fileName, content);
    }
}