package com.flashcart.inventory.product;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
public class StockReservationService {

    private static final int MAX_ATTEMPTS = 10;

    private final ProductService productService;

    public StockReservationService(ProductService productService) {
        this.productService = productService;
    }

    public Product reserve(Long productId, int quantity) {
        return withRetry(productId, () -> productService.reserve(productId, quantity));
    }

    public OrderReservation reserveForOrder(Long orderId, Long productId, int quantity) {
        return withRetry(productId, () -> productService.reserveForOrder(orderId, productId, quantity));
    }

    private <T> T withRetry(Long productId, Supplier<T> attempt) {
        for (int i = 1; ; i++) {
            try {
                return attempt.get();
            } catch (ObjectOptimisticLockingFailureException e) {
                if (i == MAX_ATTEMPTS) {
                    throw new StockConflictException(productId);
                }
                backOff(i, productId);
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