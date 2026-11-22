package com.flashcart.order.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

public record PlaceOrderRequest(
        @NotNull @Positive Long productId,
        @Positive @Max(5) int quantity
) {}
