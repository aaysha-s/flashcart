package com.flashcart.inventory.product;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "order_reservations")
public class OrderReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationOutcome outcome;

    @Column(name = "total_paise")
    private Long totalPaise;

    @Column(length = 200)
    private String reason;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected OrderReservation() {
        // required by JPA
    }

    private OrderReservation(Long orderId, Long productId, int quantity,
                             ReservationOutcome outcome, Long totalPaise, String reason) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.outcome = outcome;
        this.totalPaise = totalPaise;
        this.reason = reason;
    }

    public static OrderReservation reserved(Long orderId, Long productId, int quantity, long totalPaise) {
        return new OrderReservation(orderId, productId, quantity, ReservationOutcome.RESERVED, totalPaise, null);
    }

    public static OrderReservation rejected(Long orderId, Long productId, int quantity, String reason) {
        String safeReason = reason == null ? "Rejected" : reason.substring(0, Math.min(reason.length(), 200));
        return new OrderReservation(orderId, productId, quantity, ReservationOutcome.REJECTED, null, safeReason);
    }

    @PrePersist
    void onCreate() {
        this.processedAt = Instant.now();
    }

    public boolean isReserved() {
        return outcome == ReservationOutcome.RESERVED;
    }

    public Long getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public ReservationOutcome getOutcome() { return outcome; }
    public Long getTotalPaise() { return totalPaise; }
    public String getReason() { return reason; }
}
