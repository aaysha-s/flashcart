package com.flashcart.order.messaging;

import com.flashcart.order.TestcontainersConfiguration;
import com.flashcart.order.domain.Order;
import com.flashcart.order.domain.OrderRepository;
import com.flashcart.order.domain.OrderStatus;
import com.flashcart.order.outbox.OutboxRepository;
import com.flashcart.order.service.OrderService;
import com.flashcart.order.web.dto.PlaceOrderRequest;
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
class OrderSagaIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void placingAnOrderWritesOutboxEventThatTheRelayPublishes() {
        Order order = orderService.place(7L, new PlaceOrderRequest(1L, 2));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(outboxRepository.findAll())
                .anySatisfy(event -> assertThat(event.getPayload()).contains("\"orderId\":" + order.getId()));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(outboxRepository.findAll())
                        .filteredOn(event -> event.getPayload().contains("\"orderId\":" + order.getId()))
                        .allSatisfy(event -> assertThat(event.getPublishedAt()).isNotNull()));
    }

    @Test
    void stockReservedWaitsForPaymentThenPaymentCompletedConfirms() throws Exception {
        Long orderId = orderRepository.save(new Order(7L, 1L, 2)).getId();

        kafkaTemplate.send(Topics.STOCK_RESERVED, String.valueOf(orderId),
                "{\"orderId\":%d,\"productId\":1,\"quantity\":2,\"totalPaise\":599800}".formatted(orderId)).get();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.AWAITING_PAYMENT));

        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, String.valueOf(orderId),
                "{\"orderId\":%d,\"amountPaise\":599800}".formatted(orderId)).get();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED));
        assertThat(orderRepository.findById(orderId).orElseThrow().getTotalPaise()).isEqualTo(599800L);

        kafkaTemplate.send(Topics.STOCK_REJECTED, String.valueOf(orderId),
                "{\"orderId\":%d,\"productId\":1,\"reason\":\"late\"}".formatted(orderId)).get();
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED));
    }

    @Test
    void paymentFailedCancelsOrderWithReason() throws Exception {
        Long orderId = orderRepository.save(new Order(7L, 1L, 2)).getId();

        kafkaTemplate.send(Topics.STOCK_RESERVED, String.valueOf(orderId),
                "{\"orderId\":%d,\"productId\":1,\"quantity\":2,\"totalPaise\":599800}".formatted(orderId)).get();
        kafkaTemplate.send(Topics.PAYMENT_FAILED, String.valueOf(orderId),
                "{\"orderId\":%d,\"productId\":1,\"quantity\":2,\"reason\":\"Card declined\"}".formatted(orderId)).get();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getFailureReason()).isEqualTo("Payment failed: Card declined");
        });
    }

    @Test
    void paymentResultArrivingBeforeStockReservedStillConfirms() throws Exception {
        Long orderId = orderRepository.save(new Order(7L, 1L, 1)).getId();

        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, String.valueOf(orderId),
                "{\"orderId\":%d,\"amountPaise\":199900}".formatted(orderId)).get();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED));

        kafkaTemplate.send(Topics.STOCK_RESERVED, String.valueOf(orderId),
                "{\"orderId\":%d,\"productId\":1,\"quantity\":1,\"totalPaise\":199900}".formatted(orderId)).get();
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED));
    }

    private OrderStatus statusOf(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow().getStatus();
    }
}