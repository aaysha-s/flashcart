package com.flashcart.inventory.web.dto;

import com.flashcart.inventory.product.Product;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        long pricePaise,
        int stock,
        Instant createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPricePaise(), product.getStock(), product.getCreatedAt());
    }
}
