package com.flashcart.inventory.messaging;

public record StockChangedEvent(Long productId, int stock, long changedAtEpochMs) {}