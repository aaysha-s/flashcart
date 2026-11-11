package com.flashcart.order.messaging;

public record StockRejectedEvent(Long orderId, Long productId, String reason) {}
