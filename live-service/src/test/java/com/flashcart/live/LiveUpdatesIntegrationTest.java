package com.flashcart.live;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@Import(TestcontainersConfiguration.class)
class LiveUpdatesIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void stockChangeInKafkaReachesAConnectedBrowser() throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        WebSocketSession session = new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                        received.add(message.getPayload());
                    }
                }, "ws://localhost:" + port + "/ws/stock")
                .get(10, TimeUnit.SECONDS);

        kafkaTemplate.send("stock.changed", "42",
                "{\"productId\":42,\"stock\":9,\"changedAtEpochMs\":1700000000000}").get();

        String message = received.poll(30, TimeUnit.SECONDS);
        assertThat(message)
                .contains("\"type\":\"stock\"")
                .contains("\"productId\":42")
                .contains("\"stock\":9");

        session.close();
    }
}