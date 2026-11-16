package com.flashcart.payment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flashcart.payment")
public record PaymentProperties(double failureRate, long processingDelayMs) {
}
