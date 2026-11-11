package com.flashcart.inventory.messaging;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity) {}
