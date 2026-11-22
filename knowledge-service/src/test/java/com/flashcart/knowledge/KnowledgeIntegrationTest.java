package com.flashcart.knowledge;

import com.flashcart.knowledge.ask.AskResponse;
import com.flashcart.knowledge.ask.AskService;
import com.flashcart.knowledge.ingest.IngestionResult;
import com.flashcart.knowledge.ingest.IngestionService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class KnowledgeIntegrationTest {

    private static final String FILE = "test-policy.docx";

    @Autowired
    IngestionService ingestionService;

    @Autowired
    VectorStore vectorStore;

    @Autowired
    AskService askService;

    @MockitoBean
    ChatModel chatModel;

    private IngestionResult firstUpload;

    private static byte[] policyBytes;

    @BeforeEach
    void ingestOnce() throws IOException {
        if (policyBytes == null) {
            policyBytes = policyDocx();
        }
        firstUpload = ingestionService.ingest(FILE, "application/docx", policyBytes);
    }

    @Test
    void sameFileUploadedTwiceIsStoredOnce() {
        IngestionResult again = ingestionService.ingest(FILE, "application/docx", policyBytes);

        assertThat(again.duplicate()).isTrue();
        assertThat(again.document().getId()).isEqualTo(firstUpload.document().getId());
        assertThat(again.document().getChunkCount()).isEqualTo(2);
    }

    @Test
    void questionRetrievesTheRightSectionWithItsCitationMetadata() {
        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query("How many months are earbuds covered by the warranty?")
                .topK(1)
                .build());

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getMetadata())
                .containsEntry("fileName", FILE)
                .containsEntry("heading", "Warranty");
        assertThat(hits.get(0).getText()).contains("12 months");
    }

    @Test
    void unrelatedQuestionIsRefusedWithoutCallingTheModel() {
        AskResponse response = askService.ask("Do you sell gift wrapping?");

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
        verifyNoInteractions(chatModel);
    }

    private static byte[] policyDocx() throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            section(document, "Warranty", "Wireless earbuds are covered by the warranty for 12 months.");
            section(document, "Shipping", "Metro cities receive delivery within 2 to 3 working days.");
            document.write(out);
            return out.toByteArray();
        }
    }

    private static void section(XWPFDocument document, String heading, String body) {
        XWPFParagraph title = document.createParagraph();
        title.setStyle("Heading1");
        title.createRun().setText(heading);
        document.createParagraph().createRun().setText(body);
    }


}