package com.flashcart.inventory.messaging;

public record StockReservedEvent(Long orderId, Long productId, int quantity, long totalPaise) {}