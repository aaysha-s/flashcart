package com.flashcart.inventory.product;

import java.time.Instant;

public class SaleNotStartedException extends RuntimeException {

    public SaleNotStartedException(Long productId, Instant startsAt) {
        super("The sale for product " + productId + " starts at " + startsAt);
    }
}