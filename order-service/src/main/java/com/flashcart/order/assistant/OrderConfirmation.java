package com.flashcart.order.assistant;

public record OrderConfirmation(Long orderId, String status, String message) {}
