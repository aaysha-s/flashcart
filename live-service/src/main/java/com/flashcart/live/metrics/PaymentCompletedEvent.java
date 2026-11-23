package com.flashcart.live.metrics;

public record PaymentCompletedEvent(Long orderId, long amountPaise) {}