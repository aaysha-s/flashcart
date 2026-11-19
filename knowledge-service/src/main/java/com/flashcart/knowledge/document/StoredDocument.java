package com.flashcart.knowledge.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "documents")
public class StoredDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, unique = true, length = 64)
    private String sha256;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected StoredDocument() {
        // required by JPA
    }

    public StoredDocument(String fileName, String contentType, String sha256,
                          long sizeBytes, Integer pageCount, int chunkCount) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.sha256 = sha256;
        this.sizeBytes = sizeBytes;
        this.pageCount = pageCount;
        this.chunkCount = chunkCount;
    }

    @PrePersist
    void onCreate() {
        this.uploadedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public String getSha256() { return sha256; }
    public long getSizeBytes() { return sizeBytes; }
    public Integer getPageCount() { return pageCount; }
    public int getChunkCount() { return chunkCount; }
    public Instant getUploadedAt() { return uploadedAt; }
}