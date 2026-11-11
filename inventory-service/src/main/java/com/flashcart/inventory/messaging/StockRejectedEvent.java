package com.flashcart.inventory.messaging;

public record StockRejectedEvent(Long orderId, Long productId, String reason) {}
