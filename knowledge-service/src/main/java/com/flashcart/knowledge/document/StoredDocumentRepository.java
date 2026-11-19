package com.flashcart.knowledge.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoredDocumentRepository extends JpaRepository<StoredDocument, Long> {

    Optional<StoredDocument> findBySha256(String sha256);

    List<StoredDocument> findAllByOrderByUploadedAtDesc();
}