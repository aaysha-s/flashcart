package com.flashcart.knowledge.parsing;

/** A piece of a document plus where it came from: a PDF page, or a DOCX heading. */
public record ParsedSection(Integer pageNumber, String heading, String text) {}