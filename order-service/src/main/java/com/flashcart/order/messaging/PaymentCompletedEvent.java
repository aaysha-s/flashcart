package com.flashcart.order.messaging;

public record PaymentCompletedEvent(Long orderId, long amountPaise) {}