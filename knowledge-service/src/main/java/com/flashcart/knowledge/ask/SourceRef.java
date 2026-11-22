package com.flashcart.knowledge.ask;

/** One cited source: which file, where in it, a short snippet and how similar it was to the question. */
public record SourceRef(int number, String fileName, Integer pageNumber, String heading,
                        String snippet, double score) {}