package com.flashcart.live;

import com.flashcart.live.metrics.MetricsSnapshot;
import com.flashcart.live.metrics.MetricsStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@Import(TestcontainersConfiguration.class)
class MetricsIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    MetricsStore metrics;

    @Test
    void orderEventsAreCountedAndSoldOutIsRecorded() throws Exception {
        kafka.send("order.placed", "7", "{\"orderId\":1,\"customerId\":1,\"productId\":7,\"quantity\":1}").get();
        kafka.send("order.placed", "7", "{\"orderId\":2,\"customerId\":2,\"productId\":7,\"quantity\":1}").get();
        kafka.send("payment.completed", "1", "{\"orderId\":1,\"amountPaise\":99900}").get();
        kafka.send("stock.rejected", "2", "{\"orderId\":2,\"productId\":7,\"reason\":\"sold out\"}").get();
        kafka.send("stock.changed", "7", "{\"productId\":7,\"stock\":0,\"changedAtEpochMs\":1700000000000}").get();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            MetricsSnapshot snapshot = metrics.snapshot(System.currentTimeMillis());
            assertThat(snapshot.totals())
                    .containsEntry("placed", 2L)
                    .containsEntry("confirmed", 1L)
                    .containsEntry("rejected", 1L);
            assertThat(snapshot.revenuePaise()).isEqualTo(99900L);
            assertThat(snapshot.soldOutAtEpochMs()).containsEntry(7L, 1700000000000L);
        });
    }

    @Test
    void metricsEndpointRequiresLogin() {
        assertThatThrownBy(() -> RestClient.create()
                .get()
                .uri("http://localhost:" + port + "/api/live/admin/metrics")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }
}