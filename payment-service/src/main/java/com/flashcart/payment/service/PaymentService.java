package com.flashcart.payment.service;

import com.flashcart.payment.domain.Payment;
import com.flashcart.payment.domain.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway gateway;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentRepository paymentRepository,
                          MockPaymentGateway gateway,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Charges an order exactly once. A repeated call for the same order returns
     * the stored result instead of charging again.
     */
    public Payment process(Long orderId, long amountPaise) {
        return paymentRepository.findByOrderId(orderId)
                .map(existing -> {
                    log.info("Duplicate payment request for order {}; replaying {}",
                            orderId, existing.getStatus());
                    return existing;
                })
                .orElseGet(() -> charge(orderId, amountPaise));
    }

    private Payment charge(Long orderId, long amountPaise) {
        ChargeResult result = gateway.charge(orderId, amountPaise);
        Payment payment = result.success()
                ? Payment.completed(orderId, amountPaise)
                : Payment.failed(orderId, amountPaise, result.failureReason());
        Payment saved = paymentRepository.save(payment);

        Counter.builder("flashcart.payments")
                .tag("outcome", saved.getStatus().name().toLowerCase())
                .register(meterRegistry)
                .increment();
        log.info("Payment for order {}: {}", orderId, saved.getStatus());
        return saved;
    }
}
