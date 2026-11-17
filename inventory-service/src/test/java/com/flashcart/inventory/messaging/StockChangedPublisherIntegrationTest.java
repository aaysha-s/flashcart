package com.flashcart.inventory.messaging;

import com.flashcart.inventory.TestcontainersConfiguration;
import com.flashcart.inventory.product.Product;
import com.flashcart.inventory.product.ProductRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StockChangedPublisherIntegrationTest {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ConsumerFactory<String, String> consumerFactory;

    @Autowired
    ProductRepository productRepository;

    @Test
    void reservingStockPublishesTheNewStockLevel() throws Exception {
        Long productId = productRepository.save(new Product("Live item", "test", 10000, 5)).getId();

        try (Consumer<String, String> consumer = consumerFactory.createConsumer("stock-changed-test", null)) {
            consumer.subscribe(List.of(Topics.STOCK_CHANGED));

            kafkaTemplate.send(Topics.ORDER_PLACED, String.valueOf(productId),
                    "{\"orderId\":900101,\"customerId\":1,\"productId\":%d,\"quantity\":2}"
                            .formatted(productId)).get();

            List<String> seen = new ArrayList<>();
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    seen.add(record.value());
                }
                return seen.stream().anyMatch(value ->
                        value.contains("\"productId\":" + productId) && value.contains("\"stock\":3"));
            });
        }
    }
}