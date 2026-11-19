package com.flashcart.knowledge.chunking;

/** One piece of text to embed, still carrying where it came from. */
public record TextChunk(int index, Integer pageNumber, String heading, String text) {}