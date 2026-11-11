package com.flashcart.order.messaging;

public record StockReservedEvent(Long orderId, Long productId, int quantity, long totalPaise) {}
