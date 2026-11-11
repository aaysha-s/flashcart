package com.flashcart.order.web.dto;

import com.flashcart.order.domain.Order;
import com.flashcart.order.domain.OrderStatus;

import java.time.Instant;

public record OrderResponse(
        Long id,
        Long productId,
        int quantity,
        OrderStatus status,
        Long totalPaise,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getProductId(), order.getQuantity(),
                order.getStatus(), order.getTotalPaise(), order.getFailureReason(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
