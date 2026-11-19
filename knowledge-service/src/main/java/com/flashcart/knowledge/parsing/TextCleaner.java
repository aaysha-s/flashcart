package com.flashcart.knowledge.parsing;

final class TextCleaner {

    private TextCleaner() {
    }

    static String clean(String raw) {
        return raw.replace("\r", "")
                .replaceAll("(?<=\\w)-\\n(?=\\w)", "")   // re-join words split across lines: "warran-\nty"
                .replaceAll("[ \\t\\u00A0]+", " ")        // collapse spaces, tabs, non-breaking spaces
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}