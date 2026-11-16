package com.flashcart.payment.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockPaymentGateway {

    private final PaymentProperties properties;

    public MockPaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    public ChargeResult charge(Long orderId, long amountPaise) {
        simulateBankLatency();
        if (ThreadLocalRandom.current().nextDouble() < properties.failureRate()) {
            return ChargeResult.declined("Payment declined by bank (simulated)");
        }
        return ChargeResult.approved();
    }

    private void simulateBankLatency() {
        try {
            Thread.sleep(properties.processingDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}