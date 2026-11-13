package com.flashcart.order.assistant;

import java.math.BigDecimal;

public record ProductSummary(Long id, String name, String description, BigDecimal priceRupees, int stock) {

    static ProductSummary from(InventoryProduct p) {
        return new ProductSummary(p.id(), p.name(), p.description(), BigDecimal.valueOf(p.pricePaise(), 2), p.stock());
    }
}
