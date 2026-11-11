package com.flashcart.inventory.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "flashcart.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class StockReservationConcurrencyTest {

    @Autowired
    StockReservationService reservationService;

    @Autowired
    ProductRepository productRepository;

    @Test
    void concurrentBuyersNeverOversell() throws Exception {
        int initialStock = 10;
        int buyers = 20;
        Long id = productRepository.save(
                new Product("Concurrency test item", "test", 10000, initialStock)).getId();

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger sold = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            results.add(pool.submit(() -> {
                startSignal.await();
                try {
                    reservationService.reserve(id, 1);
                    sold.incrementAndGet();
                } catch (InsufficientStockException e) {
                    outOfStock.incrementAndGet();
                } catch (StockConflictException e) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }

        startSignal.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        int remaining = productRepository.findById(id).orElseThrow().getStock();
        System.out.printf("sold=%d, outOfStock=%d, conflicts=%d, remaining=%d%n",
                sold.get(), outOfStock.get(), conflicts.get(), remaining);

        assertThat(sold.get()).isLessThanOrEqualTo(initialStock);
        assertThat(remaining).isEqualTo(initialStock - sold.get());
        assertThat(sold.get() + outOfStock.get() + conflicts.get()).isEqualTo(buyers);

        productRepository.deleteById(id);
    }
}
