package com.flashcart.inventory.messaging;

import com.flashcart.inventory.product.StockChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StockChangedPublisher {

    private static final Logger log = LoggerFactory.getLogger(StockChangedPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public StockChangedPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockChanged(StockChanged change) {
        StockChangedEvent event = new StockChangedEvent(
                change.productId(), change.stock(), change.changedAt().toEpochMilli());
        kafkaTemplate.send(Topics.STOCK_CHANGED, String.valueOf(change.productId()),
                        jsonMapper.writeValueAsString(event))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.warn("Could not publish stock change for product {}", change.productId(), error);
                    }
                });
    }
}
