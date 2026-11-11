package com.flashcart.inventory.product;

public class StockConflictException extends RuntimeException {

    public StockConflictException(Long productId) {
        super("Product " + productId + " is in high demand; please try again");
    }
}
