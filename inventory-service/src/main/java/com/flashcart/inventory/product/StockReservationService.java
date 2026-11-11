package com.flashcart.inventory.product;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class StockReservationService {

    private static final int MAX_ATTEMPTS = 10;

    private final ProductService productService;

    public StockReservationService(ProductService productService) {
        this.productService = productService;
    }

    public Product reserve(Long productId, int quantity) {
        for (int attempt = 1; ; attempt++) {
            try {
                return productService.reserve(productId, quantity);
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new StockConflictException(productId);
                }
                backOff(attempt, productId);
            }
        }
    }

    private void backOff(int attempt, Long productId) {
        try {
            Thread.sleep(10L * attempt + ThreadLocalRandom.current().nextInt(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StockConflictException(productId);
        }
    }
}
