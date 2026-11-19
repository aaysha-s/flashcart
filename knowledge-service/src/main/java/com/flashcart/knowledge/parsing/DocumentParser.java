package com.flashcart.knowledge.parsing;

public interface DocumentParser {

    /** True if this parser handles the file, judged by its name AND its actual bytes. */
    boolean supports(String fileName, byte[] content);

    ParsedDocument parse(String fileName, byte[] content);
}