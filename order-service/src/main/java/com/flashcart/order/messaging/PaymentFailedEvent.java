package com.flashcart.order.messaging;

public record PaymentFailedEvent(Long orderId, Long productId, int quantity, String reason) {}