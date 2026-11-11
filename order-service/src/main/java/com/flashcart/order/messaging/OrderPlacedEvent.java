package com.flashcart.order.messaging;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity) {}
