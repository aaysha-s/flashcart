package com.flashcart.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected Payment() {
        // required by JPA
    }

    private Payment(Long orderId, long amountPaise, PaymentStatus status, String failureReason) {
        this.orderId = orderId;
        this.amountPaise = amountPaise;
        this.status = status;
        this.failureReason = failureReason;
    }

    public static Payment completed(Long orderId, long amountPaise) {
        return new Payment(orderId, amountPaise, PaymentStatus.COMPLETED, null);
    }

    public static Payment failed(Long orderId, long amountPaise, String reason) {
        return new Payment(orderId, amountPaise, PaymentStatus.FAILED, reason);
    }

    @PrePersist
    void onCreate() {
        this.processedAt = Instant.now();
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public long getAmountPaise() { return amountPaise; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getProcessedAt() { return processedAt; }
}
