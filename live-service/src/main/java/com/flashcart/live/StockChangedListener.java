package com.flashcart.live;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

@Component
public class StockChangedListener {

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public StockChangedListener(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    private static final Logger log = LoggerFactory.getLogger(StockChangedListener.class);

    @KafkaListener(topics = "stock.changed")
    public void onStockChanged(String payload,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        StockChangedEvent event = jsonMapper.readValue(payload, StockChangedEvent.class);
        String message = jsonMapper.writeValueAsString(StockUpdate.from(event));
        redis.convertAndSend(Channels.LIVE, message);
        log.info("Relayed stock for product {} from partition {}", event.productId(), partition);
    }
}