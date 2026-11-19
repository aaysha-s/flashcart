package com.flashcart.knowledge.parsing;

import java.util.List;

public record ParsedDocument(String fileName, Integer pageCount, List<ParsedSection> sections) {}