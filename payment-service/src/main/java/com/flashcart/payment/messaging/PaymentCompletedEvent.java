package com.flashcart.payment.messaging;

public record PaymentCompletedEvent(Long orderId, long amountPaise) {}