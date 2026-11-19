package com.flashcart.knowledge.web;

import com.flashcart.knowledge.document.StoredDocumentRepository;
import com.flashcart.knowledge.ingest.IngestionResult;
import com.flashcart.knowledge.ingest.IngestionService;
import com.flashcart.knowledge.parsing.UnsupportedDocumentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private final IngestionService ingestionService;
    private final StoredDocumentRepository documentRepository;

    public DocumentController(IngestionService ingestionService, StoredDocumentRepository documentRepository) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new UnsupportedDocumentException("The uploaded file is empty");
        }
        // Keep only the base name: never trust a client-supplied path like "../../x.pdf"
        String fileName = StringUtils.getFilename(StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "upload")));
        String contentType = Objects.requireNonNullElse(file.getContentType(), "application/octet-stream");

        IngestionResult result = ingestionService.ingest(fileName, contentType, file.getBytes());
        DocumentResponse body = DocumentResponse.from(result.document(), result.duplicate());
        return result.duplicate()
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return documentRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(document -> DocumentResponse.from(document, false))
                .toList();
    }
}