package com.flashcart.knowledge.chunking;

import com.flashcart.knowledge.parsing.ParsedSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkerTest {

    @Test
    void shortSectionBecomesOneChunkAndKeepsItsSource() {
        List<TextChunk> chunks = new TextChunker().chunk(List.of(
                new ParsedSection(2, "Warranty", "Earbuds are covered for 12 months.")));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).index()).isZero();
        assertThat(chunks.get(0).pageNumber()).isEqualTo(2);
        assertThat(chunks.get(0).heading()).isEqualTo("Warranty");
    }

    @Test
    void longSectionIsSplitWithinTheLimitAndConsecutiveChunksOverlap() {
        String text = IntStream.rangeClosed(1, 20)
                .mapToObj(n -> "Sentence number " + n + " is about returns.")
                .collect(Collectors.joining(" "));

        List<TextChunk> chunks = new TextChunker(200, 50).chunk(List.of(new ParsedSection(1, null, text)));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text().length()).isLessThanOrEqualTo(200));
        for (int i = 0; i + 1 < chunks.size(); i++) {
            String previous = chunks.get(i).text();
            String lastSentence = previous.substring(previous.lastIndexOf("Sentence number"));
            assertThat(chunks.get(i + 1).text()).startsWith(lastSentence);
        }
    }

    @Test
    void chunksNeverCrossSectionsAndIndexesRunAcrossTheDocument() {
        List<TextChunk> chunks = new TextChunker().chunk(List.of(
                new ParsedSection(1, null, "Page one text."),
                new ParsedSection(2, null, "Page two text.")));

        assertThat(chunks).extracting(TextChunk::index).containsExactly(0, 1);
        assertThat(chunks).extracting(TextChunk::pageNumber).containsExactly(1, 2);
    }

    @Test
    void veryLongRunOnTextWithoutPunctuationIsStillSplit() {
        String runOn = "word ".repeat(200).strip();

        List<TextChunk> chunks = new TextChunker(100, 20).chunk(List.of(new ParsedSection(1, null, runOn)));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text().length()).isLessThanOrEqualTo(100));
    }

    @Test
    void overlapMustBeSmallerThanTheChunk() {
        assertThatThrownBy(() -> new TextChunker(100, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}