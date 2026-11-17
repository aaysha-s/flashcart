package com.flashcart.live;

public record StockChangedEvent(Long productId, int stock, long changedAtEpochMs) {}
