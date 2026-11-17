package com.flashcart.inventory.product;

import java.time.Instant;

public record StockChanged(Long productId, int stock, Instant changedAt) {}