package com.flashcart.order.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceOrderRequest(
        @NotNull @Positive Long productId,
        @Positive int quantity
) {}
