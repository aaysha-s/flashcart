package com.flashcart.live;

public record StockUpdate(String type, Long productId, int stock, long changedAtEpochMs) {

    public static StockUpdate from(StockChangedEvent event) {
        return new StockUpdate("stock", event.productId(), event.stock(), event.changedAtEpochMs());
    }
}