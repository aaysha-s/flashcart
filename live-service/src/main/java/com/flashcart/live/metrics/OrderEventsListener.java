package com.flashcart.live.metrics;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventsListener {

    private final MetricsStore metrics;
    private final JsonMapper jsonMapper;

    public OrderEventsListener(MetricsStore metrics, JsonMapper jsonMapper) {
        this.metrics = metrics;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = {"order.placed", "payment.completed", "payment.failed", "stock.rejected"})
    public void onEvent(String payload,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp) {
        switch (topic) {
            case "order.placed" -> metrics.record(MetricType.PLACED, timestamp, 0);
            case "payment.completed" -> metrics.record(MetricType.CONFIRMED, timestamp,
                    jsonMapper.readValue(payload, PaymentCompletedEvent.class).amountPaise());
            case "payment.failed" -> metrics.record(MetricType.PAYMENT_FAILED, timestamp, 0);
            case "stock.rejected" -> metrics.record(MetricType.REJECTED, timestamp, 0);
            default -> {
            }
        }
    }
}