package com.flashcart.payment.messaging;

public record PaymentFailedEvent(Long orderId, Long productId, int quantity, String reason) {}