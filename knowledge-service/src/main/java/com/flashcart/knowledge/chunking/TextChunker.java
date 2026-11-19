package com.flashcart.knowledge.chunking;

import com.flashcart.knowledge.parsing.ParsedSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class TextChunker {

    static final int DEFAULT_MAX_CHARS = 800;
    static final int DEFAULT_OVERLAP_CHARS = 150;

    /** Split after . ! ? followed by whitespace, or at line breaks. */
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+|\\n+");

    private final int maxChars;
    private final int overlapChars;
    private final int maxSentenceChars;

    public TextChunker() {
        this(DEFAULT_MAX_CHARS, DEFAULT_OVERLAP_CHARS);
    }

    TextChunker(int maxChars, int overlapChars) {
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("Overlap must be at least 0 and smaller than the chunk size");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
        this.maxSentenceChars = maxChars - overlapChars - 1;
    }

    /** Chunks never cross a section, so every chunk keeps an exact page or heading for its citation. */
    public List<TextChunk> chunk(List<ParsedSection> sections) {
        List<TextChunk> chunks = new ArrayList<>();
        for (ParsedSection section : sections) {
            for (String piece : split(section.text())) {
                chunks.add(new TextChunk(chunks.size(), section.pageNumber(), section.heading(), piece));
            }
        }
        return chunks;
    }

    List<String> split(String text) {
        List<String> pieces = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String sentence : sentences(text)) {
            if (!current.isEmpty() && joinedLength(current) + 1 + sentence.length() > maxChars) {
                pieces.add(String.join(" ", current));
                current = overlapTail(current);
            }
            current.add(sentence);
        }
        if (!current.isEmpty()) {
            pieces.add(String.join(" ", current));
        }
        return pieces;
    }

    private List<String> sentences(String text) {
        List<String> result = new ArrayList<>();
        for (String raw : SENTENCE_BOUNDARY.split(text)) {
            String sentence = raw.strip();
            if (sentence.isEmpty()) {
                continue;
            }
            if (sentence.length() <= maxSentenceChars) {
                result.add(sentence);
            } else {
                result.addAll(hardSplit(sentence));
            }
        }
        return result;
    }

    /** The last few sentences of a chunk, up to the overlap size, carried into the next chunk. */
    private List<String> overlapTail(List<String> sentences) {
        LinkedList<String> tail = new LinkedList<>();
        int length = 0;
        for (int i = sentences.size() - 1; i >= 0; i--) {
            String sentence = sentences.get(i);
            int withSentence = length == 0 ? sentence.length() : length + 1 + sentence.length();
            if (withSentence > overlapChars) {
                break;
            }
            tail.addFirst(sentence);
            length = withSentence;
        }
        return new ArrayList<>(tail);
    }

    /** A "sentence" longer than a chunk (a table row, a run-on line) is split at word boundaries. */
    private List<String> hardSplit(String sentence) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        for (String word : sentence.split("\\s+")) {
            if (part.length() > 0 && part.length() + 1 + word.length() > maxSentenceChars) {
                parts.add(part.toString());
                part.setLength(0);
            }
            if (word.length() > maxSentenceChars) {
                for (int i = 0; i < word.length(); i += maxSentenceChars) {
                    parts.add(word.substring(i, Math.min(word.length(), i + maxSentenceChars)));
                }
                continue;
            }
            if (part.length() > 0) {
                part.append(' ');
            }
            part.append(word);
        }
        if (part.length() > 0) {
            parts.add(part.toString());
        }
        return parts;
    }

    private static int joinedLength(List<String> sentences) {
        int length = Math.max(0, sentences.size() - 1);
        for (String sentence : sentences) {
            length += sentence.length();
        }
        return length;
    }
}