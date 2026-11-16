package com.flashcart.payment.messaging;

public record StockReservedEvent(Long orderId, Long productId, int quantity, long totalPaise) {}