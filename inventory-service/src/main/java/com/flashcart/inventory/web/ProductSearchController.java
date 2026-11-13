package com.flashcart.inventory.web;

import com.flashcart.inventory.product.ProductSearchService;
import com.flashcart.inventory.web.dto.ProductResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/search")
public class ProductSearchController {

    private final ProductSearchService searchService;

    public ProductSearchController(ProductSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<ProductResponse> search(@RequestParam String q,
                                        @RequestParam(defaultValue = "5") int limit) {
        return searchService.search(q, Math.min(limit, 20)).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> reindex() {
        return Map.of("indexed", searchService.reindexAll());
    }
}