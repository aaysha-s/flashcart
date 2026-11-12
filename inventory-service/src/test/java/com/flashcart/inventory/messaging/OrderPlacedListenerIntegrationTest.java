package com.flashcart.inventory.messaging;

import com.flashcart.inventory.TestcontainersConfiguration;
import com.flashcart.inventory.product.OrderReservation;
import com.flashcart.inventory.product.OrderReservationRepository;
import com.flashcart.inventory.product.Product;
import com.flashcart.inventory.product.ProductRepository;
import com.flashcart.inventory.product.ReservationOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderPlacedListenerIntegrationTest {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderReservationRepository reservationRepository;

    @Test
    void duplicateOrderPlacedReservesStockOnlyOnce() throws Exception {
        Long productId = productRepository.save(new Product("Saga item", "test", 10000, 5)).getId();
        long orderId = 900_001L;
        String event = orderPlaced(orderId, productId, 2);

        kafkaTemplate.send(Topics.ORDER_PLACED, String.valueOf(productId), event).get();
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(reservationRepository.findByOrderId(orderId)).isPresent());
        assertThat(stockOf(productId)).isEqualTo(3);

        kafkaTemplate.send(Topics.ORDER_PLACED, String.valueOf(productId), event).get();
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(stockOf(productId)).isEqualTo(3));
    }

    @Test
    void orderLargerThanStockIsRejectedAndStockUntouched() throws Exception {
        Long productId = productRepository.save(new Product("Scarce item", "test", 10000, 2)).getId();
        long orderId = 900_002L;

        kafkaTemplate.send(Topics.ORDER_PLACED, String.valueOf(productId), orderPlaced(orderId, productId, 100)).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(reservationRepository.findByOrderId(orderId)).isPresent());
        OrderReservation reservation = reservationRepository.findByOrderId(orderId).orElseThrow();
        assertThat(reservation.getOutcome()).isEqualTo(ReservationOutcome.REJECTED);
        assertThat(stockOf(productId)).isEqualTo(2);
    }

    private String orderPlaced(long orderId, Long productId, int quantity) {
        return """
                {"orderId":%d,"customerId":1,"productId":%d,"quantity":%d}
                """.formatted(orderId, productId, quantity).strip();
    }

    private int stockOf(Long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }
}
