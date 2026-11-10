package com.flashcart.inventory.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(@PositiveOrZero int stock) {}
