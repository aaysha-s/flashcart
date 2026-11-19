package com.flashcart.knowledge.web;

import com.flashcart.knowledge.document.StoredDocument;

import java.time.Instant;

public record DocumentResponse(Long id, String fileName, Integer pageCount, int chunkCount,
                               Instant uploadedAt, boolean duplicate) {

    public static DocumentResponse from(StoredDocument document, boolean duplicate) {
        return new DocumentResponse(document.getId(), document.getFileName(), document.getPageCount(),
                document.getChunkCount(), document.getUploadedAt(), duplicate);
    }
}