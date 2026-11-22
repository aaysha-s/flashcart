package com.flashcart.knowledge.ask;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AskServiceCitationTest {

    private final List<SourceRef> sources = List.of(
            new SourceRef(1, "a.pdf", 1, null, "one", 0.9),
            new SourceRef(2, "b.pdf", 2, null, "two", 0.8),
            new SourceRef(3, "c.docx", null, "Refunds", "three", 0.7));

    @Test
    void onlyCitedSourcesAreReturnedInOrder() {
        assertThat(AskService.citedOnly("Refunds take 2-3 days [3]. Cards take longer [1].", sources))
                .extracting(SourceRef::number)
                .containsExactly(1, 3);
    }

    @Test
    void answerWithoutCitationsFallsBackToAllSources() {
        assertThat(AskService.citedOnly("Refunds take 2-3 days.", sources)).hasSize(3);
    }

    @Test
    void citationNumbersThatDoNotExistAreIgnored() {
        assertThat(AskService.citedOnly("See [2] and [9].", sources))
                .extracting(SourceRef::number)
                .containsExactly(2);
    }
}