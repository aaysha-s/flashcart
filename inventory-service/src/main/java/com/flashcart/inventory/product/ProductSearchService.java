package com.flashcart.inventory.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final VectorStore vectorStore;
    private final ProductRepository productRepository;

    public ProductSearchService(VectorStore vectorStore, ProductRepository productRepository) {
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
    }

    public void index(Product product) {
        vectorStore.add(List.of(toDocument(product)));
    }

    public int reindexAll() {
        List<Document> documents = productRepository.findAll().stream().map(this::toDocument).toList();
        vectorStore.add(documents);
        return documents.size();
    }

    public List<Product> search(String query, int limit) {
        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(limit)
                .build());
        List<Long> ids = hits.stream()
                .map(doc -> ((Number) doc.getMetadata().get("productId")).longValue())
                .toList();
        Map<Long, Product> byId = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private Document toDocument(Product product) {
        String text = product.getName() + ". " + Objects.toString(product.getDescription(), "");
        return Document.builder()
                .id(documentId(product.getId()))
                .text(text)
                .metadata(Map.of("productId", product.getId()))
                .build();
    }

    private static String documentId(Long productId) {
        return UUID.nameUUIDFromBytes(("product-" + productId).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
