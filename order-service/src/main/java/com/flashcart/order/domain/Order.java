package com.flashcart.order.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_paise")
    private Long totalPaise;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
        // required by JPA
    }

    public Order(Long customerId, Long productId, int quantity) {
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    /** Still in progress: stock or payment not settled yet. */
    public boolean isOpen() {
        return status == OrderStatus.PENDING || status == OrderStatus.AWAITING_PAYMENT;
    }

    public void markAwaitingPayment(long totalPaise) {
        if (!isPending()) {
            throw new IllegalStateException("Order " + id + " is already " + status);
        }
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.totalPaise = totalPaise;
    }

    public void confirm(long totalPaise) {
        requireOpen();
        this.status = OrderStatus.CONFIRMED;
        this.totalPaise = totalPaise;
    }

    public void cancel(String reason) {
        requireOpen();
        this.status = OrderStatus.CANCELLED;
        this.failureReason = reason;
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("Order " + id + " is already " + status);
        }
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public Long getTotalPaise() { return totalPaise; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}