package com.flashcart.inventory.messaging;

public record PaymentFailedEvent(Long orderId, Long productId, int quantity, String reason) {}
