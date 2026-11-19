package com.flashcart.knowledge.ingest;

import com.flashcart.knowledge.document.StoredDocument;

public record IngestionResult(StoredDocument document, boolean duplicate) {}