package com.flashcart.knowledge.ingest;

import com.flashcart.knowledge.chunking.TextChunk;
import com.flashcart.knowledge.chunking.TextChunker;
import com.flashcart.knowledge.document.StoredDocument;
import com.flashcart.knowledge.document.StoredDocumentRepository;
import com.flashcart.knowledge.parsing.DocumentParsers;
import com.flashcart.knowledge.parsing.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final DocumentParsers parsers;
    private final TextChunker chunker;
    private final StoredDocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public IngestionService(DocumentParsers parsers, TextChunker chunker,
                            StoredDocumentRepository documentRepository, VectorStore vectorStore) {
        this.parsers = parsers;
        this.chunker = chunker;
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * Parses, chunks, embeds and stores a document. The document row and its vectors are
     * written in one transaction, so a failed embedding call leaves nothing half-stored.
     */
    @Transactional
    public IngestionResult ingest(String fileName, String contentType, byte[] content) {
        String sha256 = sha256(content);
        Optional<StoredDocument> existing = documentRepository.findBySha256(sha256);
        if (existing.isPresent()) {
            log.info("Skipping {}: identical to document {}", fileName, existing.get().getId());
            return new IngestionResult(existing.get(), true);
        }

        ParsedDocument parsed = parsers.parse(fileName, content);
        List<TextChunk> chunks = chunker.chunk(parsed.sections());

        StoredDocument stored = documentRepository.save(new StoredDocument(
                fileName, contentType, sha256, content.length, parsed.pageCount(), chunks.size()));
        vectorStore.add(chunks.stream().map(chunk -> toVectorDocument(stored, chunk)).toList());

        log.info("Ingested {} as document {}: {} sections, {} chunks",
                fileName, stored.getId(), parsed.sections().size(), chunks.size());
        return new IngestionResult(stored, false);
    }

    private static Document toVectorDocument(StoredDocument stored, TextChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", stored.getId());
        metadata.put("fileName", stored.getFileName());
        metadata.put("chunkIndex", chunk.index());
        if (chunk.pageNumber() != null) {
            metadata.put("pageNumber", chunk.pageNumber());
        }
        if (chunk.heading() != null) {
            metadata.put("heading", chunk.heading());
        }
        // The heading is embedded with the text: "Refunds\n UPI 2-3 days" matches "refund time" better
        String text = chunk.heading() == null ? chunk.text() : chunk.heading() + "\n" + chunk.text();
        String id = UUID.nameUUIDFromBytes(
                ("doc-" + stored.getId() + "-chunk-" + chunk.index()).getBytes(StandardCharsets.UTF_8)).toString();
        return Document.builder().id(id).text(text).metadata(metadata).build();
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}