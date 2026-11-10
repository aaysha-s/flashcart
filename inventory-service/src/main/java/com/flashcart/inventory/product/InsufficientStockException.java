package com.flashcart.inventory.product;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Product " + productId + ": requested " + requested + ", only " + available + " left");
    }
}